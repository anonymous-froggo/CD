package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.Visitor;

public final record ParamTree(TypeTree type, NameTree name) implements Tree {

    @Override
    public Span span() {
        return type.span().merge(name.span());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }

}
