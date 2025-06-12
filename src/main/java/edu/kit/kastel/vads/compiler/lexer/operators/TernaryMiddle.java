package edu.kit.kastel.vads.compiler.lexer.operators;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.ExpressionTree;

public final class TernaryMiddle extends BinaryOperator {

    private final ExpressionTree expression;

    public TernaryMiddle(ExpressionTree expression, Span span) {
        super(BinaryOperatorType.TERNARY, span);

        this.expression = expression;
    }

    public ExpressionTree expression() {
        return this.expression;
    }
}
