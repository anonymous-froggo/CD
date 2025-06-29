package edu.kit.kastel.vads.compiler.lexer.keywords;

import edu.kit.kastel.vads.compiler.Span;

public final record LibFunctionKeyword(LibFunctionKeywordType type, Span span) implements Keyword {

    public enum LibFunctionKeywordType implements KeywordType {
        PRINT("print"),
        READ("read"),
        FLUSH("flush"),
        
        ALLOC("alloc"),
        ALLOC_ARRAY("alloc_array"),
        ASSERT("assert");

        private final String keyword;

        LibFunctionKeywordType(String keyword) {
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
