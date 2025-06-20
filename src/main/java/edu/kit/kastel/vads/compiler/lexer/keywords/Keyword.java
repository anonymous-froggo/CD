package edu.kit.kastel.vads.compiler.lexer.keywords;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.lexer.Token;
import edu.kit.kastel.vads.compiler.lexer.keywords.BoolKeyword.BoolKeywordType;
import edu.kit.kastel.vads.compiler.lexer.keywords.ControlKeyword.ControlKeywordType;
import edu.kit.kastel.vads.compiler.lexer.keywords.LibFunctionKeyword.LibFunctionKeywordType;
import edu.kit.kastel.vads.compiler.lexer.keywords.TypeKeyword.TypeKeywordType;

public sealed interface Keyword extends Token permits
        BoolKeyword,
        ControlKeyword,
        LibFunctionKeyword,
        TypeKeyword {

    public static Keyword fromString(String string, Span span) {
        for (BoolKeywordType value : BoolKeywordType.values()) {
            if (value.keyword().equals(string)) {
                return new BoolKeyword(value, span);
            }
        }
        for (ControlKeywordType value : ControlKeywordType.values()) {
            if (value.keyword().equals(string)) {
                return new ControlKeyword(value, span);
            }
        }
        for (LibFunctionKeywordType value : LibFunctionKeywordType.values()) {
            if (value.keyword().equals(string)) {
                return new LibFunctionKeyword(value, span);
            }
        }
        for (TypeKeywordType value : TypeKeywordType.values()) {
            if (value.keyword().equals(string)) {
                return new TypeKeyword(value, span);
            }
        }

        return null;
    }

    KeywordType type();

    @Override
    default boolean isKeyword(KeywordType keywordType) {
        return this.type() == keywordType;
    }

    @Override
    default String asString() {
        return type().toString();
    }

    sealed interface KeywordType permits BoolKeywordType, ControlKeywordType, LibFunctionKeywordType, TypeKeywordType {

        public String keyword();
    }
}
