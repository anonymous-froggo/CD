package edu.kit.kastel.vads.compiler.lexer;

import java.util.List;

import edu.kit.kastel.vads.compiler.Span;

public record Keyword(KeywordType type, Span span) implements Token {
    @Override
    public boolean isKeyword(KeywordType keywordType) {
        return this.type() == keywordType;
    }

    @Override
    public boolean isControlKeyword() {
        return KeywordType.CONTROL_KEYWORDS.contains(type());
    }

    @Override
    public boolean isTypeKeyword() {
        return KeywordType.TYPE_KEYWORDS.contains(type());
    }

    @Override
    public String asString() {
        return type().toString();
    }

    public enum KeywordType {
        STRUCT("struct"),
        IF("if"),
        ELSE("else"),
        WHILE("while"),
        FOR("for"),
        CONTINUE("continue"),
        BREAK("break"),
        RETURN("return"),
        ASSERT("assert"),
        TRUE("true"),
        FALSE("false"),
        NULL("NULL"),
        PRINT("print"),
        READ("read"),
        ALLOC("alloc"),
        ALLOC_ARRAY("alloc_array"),
        INT("int"),
        BOOL("bool"),
        VOID("void"),
        CHAR("char"),
        STRING("string"),
        ;

        public static final List<KeywordType> CONTROL_KEYWORDS = List.of(
            IF,
            WHILE,
            FOR,
            CONTINUE,
            BREAK,
            RETURN
        );
        
        public static final List<KeywordType> TYPE_KEYWORDS = List.of(
            INT,
            BOOL
        );

        private final String keyword;

        KeywordType(String keyword) {
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
