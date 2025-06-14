package edu.kit.kastel.vads.compiler.parser.ast.statements;

import org.jspecify.annotations.Nullable;

import edu.kit.kastel.vads.compiler.Position;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.Visitor;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.ExpressionTree;

public record ForTree(
    @Nullable StatementTree initializer,
    ExpressionTree condition,
    @Nullable StatementTree postBody,
    StatementTree body,
    Position start
) implements StatementTree
{

    @Override
    public Span span() {
        return new Span.SimpleSpan(start(), body().span().end());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

}
