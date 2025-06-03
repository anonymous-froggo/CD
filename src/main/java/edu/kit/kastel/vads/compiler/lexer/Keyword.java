package edu.kit.kastel.vads.compiler.lexer;

public sealed interface Keyword extends Token permits BoolKeyword, ControlKeyword, TypeKeyword {

    public KeywordType type();

    @Override
    default boolean isKeyword(KeywordType keywordType) {
        return this.type() == keywordType;
    }

    @Override
    default String asString() {
        return type().toString();
    }

    public sealed interface KeywordType permits
        BoolKeyword.BoolKeywordType,
        ControlKeyword.ControlKeywordType,
        TypeKeyword.TypeKeywordType {

        public String getKeyword();
    }
}
