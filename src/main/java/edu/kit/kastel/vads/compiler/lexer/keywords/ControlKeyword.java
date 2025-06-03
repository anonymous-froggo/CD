package edu.kit.kastel.vads.compiler.lexer.keywords;

import edu.kit.kastel.vads.compiler.Span;

public record ControlKeyword(ControlKeywordType type, Span span) implements Keyword {

    public enum ControlKeywordType implements KeywordType {

        IF("if"),
        ELSE("else"),
        WHILE("while"),
        FOR("for"),
        CONTINUE("continue"),
        BREAK("break"),
        RETURN("return");

        private final String keyword;

        ControlKeywordType(String keyword) {
            this.keyword = keyword;
        }

        public String getKeyword() {
            return this.keyword;
        }

        @Override
        public String toString() {
            return getKeyword();
        }
    }
}
