package edu.kit.kastel.vads.compiler.lexer;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.lexer.BoolKeyword.BoolKeywordType;
import edu.kit.kastel.vads.compiler.lexer.ControlKeyword.ControlKeywordType;
import edu.kit.kastel.vads.compiler.lexer.TypeKeyword.TypeKeywordType;

public sealed interface Keyword extends Token permits BoolKeyword, ControlKeyword, TypeKeyword {
    
    public static Keyword fromString(String string, Span span) {
        for (BoolKeywordType value : BoolKeywordType.values()) {
            if (value.getKeyword().equals(string)) {
                return new BoolKeyword(value, span);
            }
        }
        for (ControlKeywordType value : ControlKeywordType.values()) {
            if (value.getKeyword().equals(string)) {
                return new ControlKeyword(value, span);

            }
        }
        for (TypeKeywordType value : TypeKeywordType.values()) {
            if (value.getKeyword().equals(string)) {
                return new TypeKeyword(value, span);
            }
        }

        return null;
    }

    public KeywordType type();

    @Override
    default boolean isKeyword(KeywordType keywordType) {
        return this.type() == keywordType;
    }

    @Override
    default String asString() {
        return type().toString();
    }

    public sealed interface KeywordType permits BoolKeywordType, ControlKeywordType, TypeKeywordType {

        public String getKeyword();
    }
}
