package edu.kit.kastel.vads.compiler.lexer.keywords;

import edu.kit.kastel.vads.compiler.Span;

public record TypeKeyword(TypeKeywordType type, Span span) implements Keyword {

    public enum TypeKeywordType implements KeywordType {
        
        STRUCT("struct"),
        INT("int"),
        BOOL("bool"),
        VOID("void"),
        CHAR("char"),
        STRING("string");

        private final String keyword;

        TypeKeywordType(String keyword) {
            this.keyword = keyword;
        }

        public String getKeyword() {
            return keyword;
        }

        @Override
        public String toString() {
            return getKeyword();
        }
    }
}
