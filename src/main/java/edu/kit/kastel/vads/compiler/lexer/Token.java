package edu.kit.kastel.vads.compiler.lexer;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.lexer.Keyword.KeywordType;
import edu.kit.kastel.vads.compiler.lexer.Operator.OperatorType;
import edu.kit.kastel.vads.compiler.lexer.Separator.SeparatorType;

public sealed interface Token permits ErrorToken, Identifier, Keyword, NumberLiteral, Operator, Separator {

    Span span();

    default boolean isKeyword(KeywordType keywordType) {
        return false;
    }

    default boolean isControlKeyword() {
        return false;
    }

    default boolean isTypeKeyword() {
        return false;
    }

    default boolean isOperator(OperatorType operatorType) {
        return false;
    }

    default boolean isAssignmentOperator() {
        return false;
    }

    default boolean isSeparator(SeparatorType separatorType) {
        return false;
    }

    String asString();
}
