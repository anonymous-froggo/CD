package edu.kit.kastel.vads.compiler.parser;

import edu.kit.kastel.vads.compiler.lexer.Ident;
import edu.kit.kastel.vads.compiler.lexer.NumberLiteral;
import edu.kit.kastel.vads.compiler.lexer.Separator;
import edu.kit.kastel.vads.compiler.lexer.Separator.SeparatorType;
import edu.kit.kastel.vads.compiler.lexer.keywords.BoolKeyword;
import edu.kit.kastel.vads.compiler.lexer.keywords.ControlKeyword;
import edu.kit.kastel.vads.compiler.lexer.keywords.LibFunctionKeyword;
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
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.BoolTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.IdentTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.NumberLiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.TernaryTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.UnaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.functions.CallTree;
import edu.kit.kastel.vads.compiler.parser.ast.functions.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.functions.ParamTree;
import edu.kit.kastel.vads.compiler.parser.ast.lvalues.LValueIdentTree;
import edu.kit.kastel.vads.compiler.parser.ast.lvalues.LValueTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.DeclTree;
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

    // Entry point

    public ProgramTree parseProgram() throws ParseException {
        List<FunctionTree> functions = new ArrayList<>();
        while (this.tokenSource.hasMore()) {
            functions.add(parseFunction());
        }

        // Find main function
        for (FunctionTree function : functions) {
            if (function.name().name().asString().equals("main")) {
                FunctionTree mainFunction = function;
                return new ProgramTree(functions, mainFunction);
            }
        }

        throw new ParseException("No main method provided");
    }

    // Functions

    // Don't mind me passing the name as an argument. It's easiest that way.
    private CallTree parseCall(NameTree name) {
        // (
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);

        // ⟨arg-list⟩
        List<ExpressionTree> args = new ArrayList<>();
        while (!this.tokenSource.peek().isSeparator(SeparatorType.PAREN_CLOSE)) {
            args.add(parseExpression());

            if (this.tokenSource.peek().isSeparator(SeparatorType.COMMA)) {
                this.tokenSource.consume();
            }
        }

        // )
        this.tokenSource.consume();

        return new CallTree(name, args);
    }

    private FunctionTree parseFunction() {
        // ⟨type⟩
        TypeTree returnType = parseType();

        // ident
        Ident ident = this.tokenSource.expectIdent();

        // ⟨param-list⟩
        List<ParamTree> params = new ArrayList<>();
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        while (!this.tokenSource.peek().isSeparator(SeparatorType.PAREN_CLOSE)) {
            params.add(parseParam());

            if (this.tokenSource.peek().isSeparator(SeparatorType.COMMA)) {
                this.tokenSource.consume();
            }
        }
        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);

        // ⟨block⟩
        BlockTree body = parseBlock();

        return new FunctionTree(
                returnType,
                name(ident),
                params,
                body);
    }

    private ParamTree parseParam() {
        TypeTree type = parseType();
        NameTree name = name(this.tokenSource.expectIdent());

        return new ParamTree(type, name);
    }

    // Statements

    private BlockTree parseBlock() {
        Separator bodyOpen = this.tokenSource.expectSeparator(SeparatorType.BRACE_OPEN);
        List<StatementTree> statements = new ArrayList<>();
        while (!this.tokenSource.peek().isSeparator(SeparatorType.BRACE_CLOSE)) {
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
        return switch (this.tokenSource.peek()) {
            // ⟨decl⟩
            case TypeKeyword _ -> parseDeclaration();

            // ⟨call⟩
            case Ident ident -> parseCall(name(ident));
            case LibFunctionKeyword keyword -> parseCall(name(keyword));

            // ⟨lvalue⟩ ⟨asnop⟩ ⟨exp⟩
            default -> parseAssignment();
        };
    }

    private StatementTree parseDeclaration() {
        TypeTree type = parseType();
        Ident ident = this.tokenSource.expectIdent();
        ExpressionTree expr = null;

        if (this.tokenSource.peek().isOperator(AssignmentOperatorType.ASSIGN)) {
            // ⟨type⟩ ident = ⟨exp⟩
            this.tokenSource.consume();
            expr = parseExpression();
        }

        return new DeclTree(type, name(ident), expr);
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
                case BREAK -> {
                    this.tokenSource.consume();
                    this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
                    yield new BreakTree(keyword.span());
                }
                case CONTINUE -> {
                    this.tokenSource.consume();
                    this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
                    yield new ContinueTree(keyword.span());
                }
                case FOR -> parseFor();
                case IF -> parseIf();
                case RETURN -> parseReturn();
                case WHILE -> parseWhile();
                default -> throw new ParseException("Unexpected token '" + token + "'");
            };
        }

        throw new ParseException("Expected control keyword but got " + token);
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
        StatementTree step;
        if (this.tokenSource.peek().isSeparator(SeparatorType.PAREN_CLOSE)) {
            // No step
            step = null;
            this.tokenSource.consume();
        } else {
            step = parseSimple();
            this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
        }

        // ⟨stmt⟩
        StatementTree body = parseStatement();

        return new ForTree(initializer, condition, step, body, forToken.span().start());
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

    private StatementTree parseReturn() {
        Token returnToken = this.tokenSource.consume();
        ExpressionTree expression = parseExpression();
        this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
        return new ReturnTree(expression, returnToken.span().start());
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

    // LValues
    
    private LValueTree parseLValue() {
        if (this.tokenSource.peek().isSeparator(SeparatorType.PAREN_OPEN)) {
            // LValue is surrounded by parantheses, remove them recursively
            this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
            LValueTree inner = parseLValue();
            this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
            return inner;
        }

        // LValue is not surrounded by parantheses
        Ident ident = this.tokenSource.expectIdent();
        return new LValueIdentTree(name(ident));
    }

    // Expressions

    private ExpressionTree parseExpression() {
        return precedenceClimbing(BinaryOperator.MIN_PRECEDENCE);
    }

    private ExpressionTree precedenceClimbing(int minPrecedence) {
        ExpressionTree result = parseAtom();

        int precedence;
        Associativity associativity;

        int nextMinPrecedence;

        while (this.tokenSource.peek() instanceof BinaryOperator operator
                // This marks the end of the ternary middle block.
                // Treat it like it's not a binary operator.
                && operator.type() != BinaryOperatorType.TERNARY_CLOSE
                && operator.type().precedence() >= minPrecedence) {
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
            case BoolKeyword keyword -> new BoolTree(keyword);

            // ident | ⟨call⟩
            case Ident ident -> {
                if (this.tokenSource.peek().isSeparator(SeparatorType.PAREN_OPEN)) {
                    // ⟨call⟩
                    yield parseCall(name(ident));
                }
                // ident
                yield new IdentTree(name(ident));
            }
            // ⟨call⟩
            case LibFunctionKeyword keyword -> parseCall(name(keyword));

            // ⟨intconst⟩
            case NumberLiteral numberLiteral -> new NumberLiteralTree(
                    numberLiteral.value(), numberLiteral.base(), numberLiteral.span());

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

    // Other trees

    private TypeTree parseType() {
        Keyword type = this.tokenSource.expectKeyword();
        return new TypeTree(BasicType.fromKeyword(type), type.span());
    }

    // Helper methods

    private static NameTree name(Ident ident) {
        return new NameTree(Name.forIdent(ident), ident.span());
    }

    private static NameTree name(LibFunctionKeyword keyword) {
        return new NameTree(Name.forLibFunctionKeyword(keyword), keyword.span());
    }
}
