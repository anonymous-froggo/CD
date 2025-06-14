package edu.kit.kastel.vads.compiler.parser.ast.expressions;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.Visitor;
import edu.kit.kastel.vads.compiler.lexer.operators.UnaryOperator;

public record UnaryOperationTree(UnaryOperator operator, ExpressionTree operand) implements ExpressionTree {

    @Override
    public Span span() {
        return operator.span().merge(operand().span());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
