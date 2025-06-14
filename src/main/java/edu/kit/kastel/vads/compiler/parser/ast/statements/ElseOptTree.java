package edu.kit.kastel.vads.compiler.parser.ast.statements;

import edu.kit.kastel.vads.compiler.Position;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.Visitor;

// TODO check if I wanna implement this as a regular StatementTree or smth
public record ElseOptTree(StatementTree elseStatement, Position start) implements StatementTree {

    @Override
    public Span span() {
        return new Span.SimpleSpan(start(), elseStatement().span().end());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

}
