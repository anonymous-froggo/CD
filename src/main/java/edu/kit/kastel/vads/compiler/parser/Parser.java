package edu.kit.kastel.vads.compiler.parser;

import edu.kit.kastel.vads.compiler.lexer.Identifier;
import edu.kit.kastel.vads.compiler.lexer.Keyword;
import edu.kit.kastel.vads.compiler.lexer.Keyword.KeywordType;
import edu.kit.kastel.vads.compiler.lexer.NumberLiteral;
import edu.kit.kastel.vads.compiler.lexer.Operator;
import edu.kit.kastel.vads.compiler.lexer.Operator.OperatorType;
import edu.kit.kastel.vads.compiler.lexer.Separator;
import edu.kit.kastel.vads.compiler.lexer.Separator.SeparatorType;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.lexer.Token;
import edu.kit.kastel.vads.compiler.parser.ast.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.IdentExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.LValueIdentTree;
import edu.kit.kastel.vads.compiler.parser.ast.LValueTree;
import edu.kit.kastel.vads.compiler.parser.ast.LiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.NegateTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final TokenSource tokenSource;

    public Parser(TokenSource tokenSource) {
        this.tokenSource = tokenSource;
    }

    public ProgramTree parseProgram() throws ParseException {
        ProgramTree programTree = new ProgramTree(List.of(parseFunction()));
        if (this.tokenSource.hasMore()) {
            throw new ParseException("expected end of input but got " + this.tokenSource.peek());
        }

        // TODO: refactor this once multiple functions are supported
        for (FunctionTree functionTree : programTree.topLevelTrees()) {
            if (functionTree.name().name().asString().equals("main")) {
                return programTree;
            }
        }

        // No main function found :(
        throw new ParseException("no main function provided");
    }

    private FunctionTree parseFunction() {
        Keyword returnType = this.tokenSource.expectKeyword(KeywordType.INT);
        Identifier identifier = this.tokenSource.expectIdentifier();
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
        BlockTree body = parseBlock();
        return new FunctionTree(
            new TypeTree(BasicType.INT, returnType.span()),
            name(identifier),
            body
        );
    }

    private BlockTree parseBlock() {
        Separator bodyOpen = this.tokenSource.expectSeparator(SeparatorType.BRACE_OPEN);
        List<StatementTree> statements = new ArrayList<>();
        while (!(this.tokenSource.peek() instanceof Separator sep && sep.type() == SeparatorType.BRACE_CLOSE)) {
            statements.add(parseStatement());
        }
        Separator bodyClose = this.tokenSource.expectSeparator(SeparatorType.BRACE_CLOSE);
        return new BlockTree(statements, bodyOpen.span().merge(bodyClose.span()));
    }

    private StatementTree parseStatement() {
        Token token = this.tokenSource.peek();
        System.out.println(token.asString());
        StatementTree statement;

        if (token.isSeparator(SeparatorType.BRACE_OPEN)) {
            // ⟨block⟩
            return parseBlock();
        }

        if (token instanceof Keyword keyword) {
            // ⟨simp⟩ -> ⟨decl⟩ | ⟨control⟩
            if (keyword.isTypeKeyword()) {
                // ⟨simp⟩ -> ⟨decl⟩
                statement = parseDeclaration();
            } else {
                // ⟨control⟩
                return parseControl();
            }
        } else {
            // ⟨simp⟩ -> ⟨lvalue⟩ ⟨asnop⟩ ⟨exp⟩
            statement = parseAssignment();
        }

        // ⟨simp⟩ ;
        this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
        return statement;
    }

    private StatementTree parseDeclaration() {
        Keyword type = this.tokenSource.expectKeyword();
        Identifier ident = this.tokenSource.expectIdentifier();
        ExpressionTree expr = null;

        if (this.tokenSource.peek().isOperator(OperatorType.ASSIGN)) {
            // ⟨type⟩ ident = ⟨exp⟩
            this.tokenSource.expectOperator(OperatorType.ASSIGN);
            expr = parseExpression();
        }

        return new DeclarationTree(
            new TypeTree(BasicType.fromKeyword(type), type.span()),
            name(ident),
            expr
        );
    }

    private StatementTree parseAssignment() {
        // ⟨lvalue⟩
        LValueTree lValue = parseLValue();

        // ⟨asnop⟩
        Operator assignmentOperator = parseAssignmentOperator();

        // ⟨exp⟩
        ExpressionTree expression = parseExpression();

        return new AssignmentTree(lValue, assignmentOperator, expression);
    }

    private LValueTree parseLValue() {
        if (this.tokenSource.peek().isSeparator(SeparatorType.PAREN_OPEN)) {
            // LValue is surrounded by parantheses, remove them recursively
            this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
            LValueTree inner = parseLValue();
            this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
            return inner;
        }

        // LValue is not surrounded by parantheses
        Identifier identifier = this.tokenSource.expectIdentifier();
        return new LValueIdentTree(name(identifier));
    }

    private Operator parseAssignmentOperator() {
        Token token = this.tokenSource.consume();

        if (token instanceof Operator operator && token.isAssignmentOperator()) {
            return operator;
        }

        throw new ParseException("expected assignment but got " + token);
    }

    private StatementTree parseControl() {
        Token token = this.tokenSource.peek();

        if (token instanceof Keyword keyword) {
            return switch (keyword.type()) {
                case IF -> parseIf();
                case WHILE -> parseWhile();
                case FOR -> parseFor();
                // TODO: implement continue, break
                case CONTINUE -> null;
                case BREAK -> null;
                case RETURN -> parseReturn();
                default -> throw new ParseException("expected control keyword but got " + keyword);
            };
        }

        throw new ParseException("expected control keyword but got " + token);
    }

    private StatementTree parseIf() {
        // TODO: implement
        return null;
    }

    private StatementTree parseWhile() {
        // TODO: implement
        return null;
    }

    private StatementTree parseFor() {
        // TODO: implement
        return null;
    }

    private StatementTree parseReturn() {
        Keyword ret = this.tokenSource.expectKeyword(KeywordType.RETURN);
        ExpressionTree expression = parseExpression();
        this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
        return new ReturnTree(expression, ret.span().start());
    }

    private ExpressionTree parseExpression() {
        // TODO: implement true, false, ternary
        ExpressionTree lhs = parseTerm();
        while (true) {
            if (this.tokenSource.peek() instanceof Operator(var type, _)
                && (type == OperatorType.PLUS || type == OperatorType.MINUS)) {
                this.tokenSource.consume();
                lhs = new BinaryOperationTree(lhs, parseTerm(), type);
            } else {
                return lhs;
            }
        }
    }

    private ExpressionTree parseTerm() {
        ExpressionTree lhs = parseFactor();
        while (true) {
            if (this.tokenSource.peek() instanceof Operator(var type, _)
                && (type == OperatorType.MUL || type == OperatorType.DIV || type == OperatorType.MOD)) {
                this.tokenSource.consume();
                lhs = new BinaryOperationTree(lhs, parseFactor(), type);
            } else {
                return lhs;
            }
        }
    }

    private ExpressionTree parseFactor() {
        return switch (this.tokenSource.peek()) {
            case Separator(var type, _) when type == SeparatorType.PAREN_OPEN -> {
                this.tokenSource.consume();
                ExpressionTree expression = parseExpression();
                this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
                yield expression;
            }
            case Operator(var type, _) when type == OperatorType.MINUS -> {
                Span span = this.tokenSource.consume().span();
                yield new NegateTree(parseFactor(), span);
            }
            case Identifier ident -> {
                this.tokenSource.consume();
                yield new IdentExpressionTree(name(ident));
            }
            case
                NumberLiteral(String value, int base, Span span) -> {
                this.tokenSource.consume();
                yield new LiteralTree(value, base, span);
            }
            case Token t -> throw new ParseException("invalid factor " + t);
        };
    }

    private static NameTree name(Identifier ident) {
        return new NameTree(Name.forIdentifier(ident), ident.span());
    }
}
