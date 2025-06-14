package edu.kit.kastel.vads.compiler.parser.ast.statements;

import edu.kit.kastel.vads.compiler.Position;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.Visitor;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.ExpressionTree;

public record ReturnTree(ExpressionTree expression, Position start) implements StatementTree {

    @Override
    public Span span() {
        return new Span.SimpleSpan(start(), expression().span().end());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
