package edu.kit.kastel.vads.compiler.parser.ast.expressions;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.lexer.keywords.BoolKeyword;
import edu.kit.kastel.vads.compiler.lexer.keywords.BoolKeyword.BoolKeywordType;
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

    // TODO check if 0 and 1 is really what you should use here
    public int value() {
        return switch(keyword.type()) {
            case BoolKeywordType.FALSE -> 0;
            case BoolKeywordType.TRUE -> 1;
        };
    }
}
