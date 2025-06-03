package edu.kit.kastel.vads.compiler.parser.ast.statements;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.ast.StatementTree;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record ForTree() implements StatementTree {

    @Override
    public Span span() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'span'");
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

}
