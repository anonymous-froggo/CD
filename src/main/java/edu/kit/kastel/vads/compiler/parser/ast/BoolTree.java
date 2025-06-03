package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.lexer.keywords.BoolKeyword;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record BoolTree(BoolKeyword keyword) implements ExpressionTree {

    @Override
    public Span span() {
        return keyword.span();
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
