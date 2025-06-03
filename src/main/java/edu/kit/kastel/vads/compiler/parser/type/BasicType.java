package edu.kit.kastel.vads.compiler.parser.type;

import java.util.Locale;

import edu.kit.kastel.vads.compiler.lexer.Keyword;
import edu.kit.kastel.vads.compiler.lexer.TypeKeyword.TypeKeywordType;
import edu.kit.kastel.vads.compiler.parser.ParseException;

public enum BasicType implements Type {
    INT,
    BOOL;

    @Override
    public String asString() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static BasicType fromKeyword(Keyword keyword) {
        return switch (keyword.type()) {
            case TypeKeywordType.INT -> INT;
            case TypeKeywordType.BOOL -> BOOL;
            default -> throw new ParseException("expected a basic type but got " + keyword);
        };
    }
}
