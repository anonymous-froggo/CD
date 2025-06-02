package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record IfTree() implements StatementTree {

    @Override
    public Span span() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'span'");
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }

}
