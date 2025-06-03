package edu.kit.kastel.vads.compiler.parser;

import edu.kit.kastel.vads.compiler.lexer.AssignmentOperator;
import edu.kit.kastel.vads.compiler.lexer.BinaryOperator;
import edu.kit.kastel.vads.compiler.lexer.Identifier;
import edu.kit.kastel.vads.compiler.lexer.Keyword;
import edu.kit.kastel.vads.compiler.lexer.Keyword.KeywordType;
import edu.kit.kastel.vads.compiler.lexer.NumberLiteral;
import edu.kit.kastel.vads.compiler.lexer.Operator;
import edu.kit.kastel.vads.compiler.lexer.Operator.Associativity;
import edu.kit.kastel.vads.compiler.lexer.Operator.OperatorType;
import edu.kit.kastel.vads.compiler.lexer.Separator;
import edu.kit.kastel.vads.compiler.lexer.Separator.SeparatorType;
import edu.kit.kastel.vads.compiler.Main;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.lexer.Token;
import edu.kit.kastel.vads.compiler.lexer.UnaryOperator;
import edu.kit.kastel.vads.compiler.lexer.AssignmentOperator.AssignmentOperatorType;
import edu.kit.kastel.vads.compiler.lexer.BinaryOperator.BinaryOperatorType;
import edu.kit.kastel.vads.compiler.parser.ast.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.IdentifierTree;
import edu.kit.kastel.vads.compiler.parser.ast.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.LValueIdentifierTree;
import edu.kit.kastel.vads.compiler.parser.ast.LValueTree;
import edu.kit.kastel.vads.compiler.parser.ast.NumberLiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.UnaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.BoolTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.WhileTree;
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

        if (Main.DEBUG) {
            System.out.println(Printer.print(programTree));
        }

        // TODO: refactor this once multiple functions are supported
        for (FunctionTree functionTree : programTree.topLevelTrees()) {
            if (functionTree.name().name().asString().equals("main")) {
                // found main function
                return programTree;
            }
        }

        // no main function :(
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

        if (this.tokenSource.peek().isOperator(AssignmentOperatorType.ASSIGN)) {
            // ⟨type⟩ ident = ⟨exp⟩
            this.tokenSource.consume();
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
        AssignmentOperator assignmentOperator = parseAssignmentOperator();

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
        return new LValueIdentifierTree(name(identifier));
    }

    private AssignmentOperator parseAssignmentOperator() {
        Token token = this.tokenSource.consume();

        if (token instanceof AssignmentOperator assignmentOperator) {
            return assignmentOperator;
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
                case CONTINUE -> {
                    this.tokenSource.consume();
                    this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
                    yield new ContinueTree(keyword.span());
                }
                case BREAK -> {
                    this.tokenSource.consume();
                    this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
                    yield new BreakTree(keyword.span());
                }
                case RETURN -> parseReturn();
                default -> throw new ParseException("expected control keyword but got " + keyword);
            };
        }

        throw new ParseException("expected control keyword but got " + token);
    }

    private StatementTree parseIf() {
        // TODO: implement parseIf
        return new IfTree();
    }

    private StatementTree parseWhile() {
        // TODO: implement parseWhile
        return new WhileTree();
    }

    private StatementTree parseFor() {
        // TODO: implement parseFor
        return new ForTree();
    }

    private StatementTree parseReturn() {
        Keyword ret = this.tokenSource.expectKeyword(KeywordType.RETURN);
        ExpressionTree expression = parseExpression();
        this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
        return new ReturnTree(expression, ret.span().start());
    }

    private ExpressionTree parseExpression() {
        return precedenceClimbing(0);
    }

    private ExpressionTree precedenceClimbing(int minPrecedence) {
        ExpressionTree result = parseAtom();

        int precedence;
        Associativity associativity;

        int nextMinPrecedence;

        while (this.tokenSource.peek() instanceof BinaryOperator operator
            && operator.type().getPrecedence() >= minPrecedence) {
            this.tokenSource.consume();
            precedence = operator.type().getPrecedence();
            associativity = operator.type().getAssociativity();

            nextMinPrecedence = switch (associativity) {
                case LEFT: {
                    yield precedence + 1;
                }
                case RIGHT: {
                    yield precedence;
                }
            };

            ExpressionTree rhs = precedenceClimbing(nextMinPrecedence);
            result = new BinaryOperationTree(result, rhs, operator.type());
        }

        return result;
    }

    private ExpressionTree parseAtom() {
        Token token = this.tokenSource.consume();
        ExpressionTree atom;

        if (token.isKeyword(KeywordType.TRUE)) {
            // true
            atom = new BoolTree(token.span());
        } else if (token instanceof Identifier identifier) {
            // ident
            atom = new IdentifierTree(name(identifier));
        } else if (token.isSeparator(SeparatorType.PAREN_OPEN)) {
            // ( ⟨exp⟩ )
            atom = parseExpression();
            this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
        } else if (token instanceof NumberLiteral numberLiteral) {
            // ⟨intconst⟩
            atom = new NumberLiteralTree(numberLiteral.value(), numberLiteral.base(), numberLiteral.span());
        } else if (token instanceof UnaryOperator operator) {
            // ⟨unop⟩ ⟨exp⟩
            atom = new UnaryOperationTree(operator, parseAtom());
        } else if (token instanceof BinaryOperator operator && token.isOperator(BinaryOperatorType.MINUS)) {
            // In this case BinaryOperatorType.MINUS is actually a unary minus
            atom = new UnaryOperationTree(operator, parseAtom());
        } else {
            throw new ParseException("unexpected token '" + token + "'");
        }

        return atom;
    }

    private static NameTree name(Identifier ident) {
        return new NameTree(Name.forIdentifier(ident), ident.span());
    }
}
