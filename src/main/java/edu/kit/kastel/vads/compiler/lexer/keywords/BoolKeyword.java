package edu.kit.kastel.vads.compiler.lexer.keywords;

import edu.kit.kastel.vads.compiler.Span;

public record BoolKeyword(BoolKeywordType type, Span span) implements Keyword {

    public enum BoolKeywordType implements KeywordType {

        TRUE("true"),
        FALSE("false");

        private final String keyword;

        BoolKeywordType(String keyword) {
            this.keyword = keyword;
        }

        public String keyword() {
            return keyword;
        }

        @Override
        public String toString() {
            return keyword();
        }
    }
}
