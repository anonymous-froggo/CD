package edu.kit.kastel.vads.compiler.parser.ast.statements;

import org.jspecify.annotations.Nullable;

import edu.kit.kastel.vads.compiler.Position;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record IfTree(
    ExpressionTree condition,
    StatementTree thenStatement,
    @Nullable ElseOptTree elseOpt,
    Position start
) implements StatementTree
{

    @Override
    public Span span() {
        Position end = (elseOpt() == null ? thenStatement() : elseOpt()).span().end();
        return new Span.SimpleSpan(start, end);
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
