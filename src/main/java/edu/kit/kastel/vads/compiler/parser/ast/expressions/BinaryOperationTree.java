package edu.kit.kastel.vads.compiler.parser.ast.expressions;

import edu.kit.kastel.vads.compiler.lexer.operators.BinaryOperator;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.Visitor;

public record BinaryOperationTree(ExpressionTree lhs, ExpressionTree rhs, BinaryOperator operator)
    implements ExpressionTree
{

    @Override
    public Span span() {
        return lhs().span().merge(rhs().span());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

    @Override
    public final int hashCode() {
        return System.identityHashCode(this);
    }
}
