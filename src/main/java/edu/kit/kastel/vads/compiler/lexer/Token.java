package edu.kit.kastel.vads.compiler.lexer;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.lexer.Separator.SeparatorType;
import edu.kit.kastel.vads.compiler.lexer.keywords.Keyword;
import edu.kit.kastel.vads.compiler.lexer.keywords.Keyword.KeywordType;
import edu.kit.kastel.vads.compiler.lexer.operators.Operator;
import edu.kit.kastel.vads.compiler.lexer.operators.Operator.OperatorType;

public sealed interface Token permits ErrorToken, Ident, Keyword, NumberLiteral, Operator, Separator {

    Span span();

    default boolean isKeyword(KeywordType keywordType) {
        return false;
    }

    default boolean isOperator(OperatorType operatorType) {
        return false;
    }

    default boolean isSeparator(SeparatorType separatorType) {
        return false;
    }

    String asString();
}
