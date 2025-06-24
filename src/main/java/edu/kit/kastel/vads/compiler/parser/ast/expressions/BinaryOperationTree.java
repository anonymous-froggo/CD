package edu.kit.kastel.vads.compiler.parser.ast.expressions;

import edu.kit.kastel.vads.compiler.lexer.operators.BinaryOperator;
import edu.kit.kastel.vads.compiler.parser.ast.TreeVisitor;
import edu.kit.kastel.vads.compiler.Span;

public record BinaryOperationTree(ExpressionTree lhs, ExpressionTree rhs, BinaryOperator operator)
    implements ExpressionTree
{

    @Override
    public Span span() {
        return lhs().span().merge(rhs().span());
    }

    @Override
    public <T, R> R accept(TreeVisitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
