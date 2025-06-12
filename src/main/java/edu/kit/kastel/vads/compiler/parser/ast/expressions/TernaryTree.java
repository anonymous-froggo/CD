package edu.kit.kastel.vads.compiler.parser.ast.expressions;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record TernaryTree(ExpressionTree condition, ExpressionTree thenExpression, ExpressionTree elseExpression)
    implements ExpressionTree
{

    @Override
    public Span span() {
        return condition().span().merge(elseExpression().span());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
