package edu.kit.kastel.vads.compiler.parser.ast.statements;

import edu.kit.kastel.vads.compiler.Position;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record IfTree(
    ExpressionTree condition,
    StatementTree thenStatement,
    StatementTree elseOpt, // TODO maybe use @Nullable instead of EmptyTree (see [DeclarationTree])
    Position start
) implements StatementTree
{

    @Override
    public Span span() {
        Position end = (elseOpt() instanceof EmptyTree ? thenStatement() : elseOpt()).span().end();
        return new Span.SimpleSpan(start, end);
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
