package edu.kit.kastel.vads.compiler.parser;

import edu.kit.kastel.vads.compiler.lexer.Identifier;
import edu.kit.kastel.vads.compiler.lexer.NumberLiteral;
import edu.kit.kastel.vads.compiler.lexer.Separator;
import edu.kit.kastel.vads.compiler.lexer.Separator.SeparatorType;
import edu.kit.kastel.vads.compiler.lexer.keywords.BoolKeyword;
import edu.kit.kastel.vads.compiler.lexer.keywords.ControlKeyword;
import edu.kit.kastel.vads.compiler.lexer.keywords.Keyword;
import edu.kit.kastel.vads.compiler.lexer.keywords.TypeKeyword;
import edu.kit.kastel.vads.compiler.lexer.keywords.ControlKeyword.ControlKeywordType;
import edu.kit.kastel.vads.compiler.lexer.keywords.TypeKeyword.TypeKeywordType;
import edu.kit.kastel.vads.compiler.lexer.operators.AssignmentOperator;
import edu.kit.kastel.vads.compiler.lexer.operators.BinaryOperator;
import edu.kit.kastel.vads.compiler.lexer.operators.Operator;
import edu.kit.kastel.vads.compiler.lexer.operators.TernaryMiddle;
import edu.kit.kastel.vads.compiler.lexer.operators.UnaryOperator;
import edu.kit.kastel.vads.compiler.lexer.operators.AssignmentOperator.AssignmentOperatorType;
import edu.kit.kastel.vads.compiler.lexer.operators.BinaryOperator.BinaryOperatorType;
import edu.kit.kastel.vads.compiler.lexer.operators.Operator.Associativity;
import edu.kit.kastel.vads.compiler.lexer.operators.UnaryOperator.UnaryOperatorType;
import edu.kit.kastel.vads.compiler.Main;
import edu.kit.kastel.vads.compiler.lexer.Token;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.LValueIdentifierTree;
import edu.kit.kastel.vads.compiler.parser.ast.LValueTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.BoolTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.IdentifierTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.NumberLiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.TernaryTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.UnaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ElseOptTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.WhileTree;
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
        Keyword returnType = this.tokenSource.expectKeyword(TypeKeywordType.INT);
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

        if (token.isSeparator(SeparatorType.BRACE_OPEN)) {
            // ⟨block⟩
            return parseBlock();
        }

        if (token instanceof ControlKeyword) {
            // ⟨control⟩
            return parseControl();
        }

        // ⟨simp⟩ ;
        StatementTree simple = parseSimple();
        this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
        return simple;
    }

    private StatementTree parseSimple() {
        Token token = this.tokenSource.peek();

        if (token instanceof TypeKeyword) {
            // ⟨decl⟩
            return parseDeclaration();
        }

        // ⟨lvalue⟩ ⟨asnop⟩ ⟨exp⟩
        return parseAssignment();

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

        return new AssignmentTree(lValue, assignmentOperator.type(), expression);
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

        if (token instanceof ControlKeyword keyword) {
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
                default -> throw new ParseException("Unexpected token '" + token + "'");
            };
        }

        throw new ParseException("expected control keyword but got " + token);
    }

    private StatementTree parseIf() {
        // if
        Token ifToken = this.tokenSource.consume();

        // ( ⟨exp⟩ )
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        ExpressionTree condition = parseExpression();
        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);

        // ⟨stmt⟩
        StatementTree thenStatement = parseStatement();

        // ⟨elseopt⟩
        ElseOptTree elseOpt = parseElseOpt();

        return new IfTree(condition, thenStatement, elseOpt, ifToken.span().start());
    }

    private ElseOptTree parseElseOpt() {
        Token token = this.tokenSource.peek();
        if (!token.isKeyword(ControlKeywordType.ELSE)) {
            // 𝜀
            return null;
        }

        // else ⟨stmt⟩
        this.tokenSource.consume();
        StatementTree elseStatement = parseStatement();
        return new ElseOptTree(elseStatement, token.span().start());
    }

    private StatementTree parseWhile() {
        // while
        Token whileToken = this.tokenSource.consume();

        // ( ⟨exp⟩ )
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        ExpressionTree condition = parseExpression();
        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);

        // ⟨stmt⟩
        StatementTree body = parseStatement();

        return new WhileTree(condition, body, whileToken.span().start());
    }

    private StatementTree parseFor() {
        // for
        Token forToken = this.tokenSource.consume();

        // (
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);

        // ⟨simpopt⟩
        StatementTree initializer;
        if (this.tokenSource.peek().isSeparator(SeparatorType.SEMICOLON)) {
            // No initializer
            initializer = null;
            this.tokenSource.consume();
        } else {
            initializer = parseSimple();
            this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
        }

        // ⟨exp⟩
        ExpressionTree condition = parseExpression();
        this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);

        // ⟨simpopt⟩
        StatementTree postBody;
        if (this.tokenSource.peek().isSeparator(SeparatorType.PAREN_CLOSE)) {
            // No initializer
            postBody = null;
            this.tokenSource.consume();
        } else {
            postBody = parseSimple();
            this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
        }

        // ⟨stmt⟩
        StatementTree body = parseStatement();

        return new ForTree(initializer, condition, postBody, body, forToken.span().start());
    }

    private StatementTree parseReturn() {
        Token returnToken = this.tokenSource.consume();
        ExpressionTree expression = parseExpression();
        this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
        return new ReturnTree(expression, returnToken.span().start());
    }

    private ExpressionTree parseExpression() {
        return precedenceClimbing(BinaryOperator.MIN_PRECEDENCE);
    }

    private ExpressionTree precedenceClimbing(int minPrecedence) {
        ExpressionTree result = parseAtom();

        int precedence;
        Associativity associativity;

        int nextMinPrecedence;

        while (
            this.tokenSource.peek() instanceof BinaryOperator operator
                // This marks the end of the ternary middle block.
                // Treat it like it's not a binary operator.
                && operator.type() != BinaryOperatorType.TERNARY_CLOSE
                && operator.type().precedence() >= minPrecedence
        ) {
            operator = parseBinaryOperator(operator);

            precedence = operator.type().precedence();
            associativity = operator.type().associativity();

            nextMinPrecedence = switch (associativity) {
                case LEFT: {
                    yield precedence + 1;
                }
                case RIGHT: {
                    yield precedence;
                }
            };

            ExpressionTree rhs = precedenceClimbing(nextMinPrecedence);

            result = operator instanceof TernaryMiddle ternaryMiddle
                ? new TernaryTree(result, ternaryMiddle.expression(), rhs)
                : new BinaryOperationTree(result, rhs, operator);
        }

        return result;
    }

    // Returns null if the the next token is not a binary operator.
    private BinaryOperator parseBinaryOperator(BinaryOperator operator) {
        this.tokenSource.consume();

        if (operator.type() == BinaryOperatorType.TERNARY_OPEN) {
            // Parse the ternary's middle expression and use it like a binary operator
            // for the ternary's start and end expression
            ExpressionTree middleExpression = precedenceClimbing(BinaryOperatorType.TERNARY_OPEN.precedence());
            Operator ternaryClose = this.tokenSource.expectOperator(BinaryOperatorType.TERNARY_CLOSE);
            return new TernaryMiddle(middleExpression, operator.span().merge(ternaryClose.span()));
        }

        return operator;
    }

    private ExpressionTree parseAtom() {
        Token token = this.tokenSource.consume();
        ExpressionTree atom;

        if (token.isSeparator(SeparatorType.PAREN_OPEN)) {
            // ( ⟨exp⟩ )
            atom = parseExpression();
            this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
            return atom;
        }

        atom = switch (token) {
            // true | false
            case BoolKeyword keyword -> new BoolTree(keyword);// ident
            case Identifier identifier -> new IdentifierTree(name(identifier));// ⟨intconst⟩
            case NumberLiteral numberLiteral -> new NumberLiteralTree(
                numberLiteral.value(), numberLiteral.base(), numberLiteral.span()
            );// TODO implement unaries using binary trees (unary trees cause trouble in
            // codegen)
            // ⟨unop⟩ ⟨exp⟩
            case UnaryOperator operator -> new UnaryOperationTree(operator, parseAtom());
            case BinaryOperator operator when operator.isOperator(BinaryOperatorType.MINUS) -> {
                // In this case BinaryOperatorType.MINUS is actually a unary minus
                UnaryOperator unaryMinus = new UnaryOperator(UnaryOperatorType.NEGATE, operator.span());
                yield new UnaryOperationTree(unaryMinus, parseAtom());
            }
            default -> throw new ParseException("unexpected token '" + token + "'");
        };
        return atom;
    }

    private static NameTree name(Identifier ident) {
        return new NameTree(Name.forIdentifier(ident), ident.span());
    }
}
