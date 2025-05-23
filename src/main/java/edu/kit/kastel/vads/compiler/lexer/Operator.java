package edu.kit.kastel.vads.compiler.lexer;

import java.util.List;

import edu.kit.kastel.vads.compiler.Span;

public record Operator(OperatorType type, Span span) implements Token {

    @Override
    public boolean isOperator(OperatorType operatorType) {
        return type() == operatorType;
    }

    @Override
    public boolean isAssignmentOperator() {
        return OperatorType.ASSIGNMENT_OPERATORS.contains(type());
    }

    @Override
    public String asString() {
        return type().toString();
    }

    public enum OperatorType {
        // Ordered by precedence from highest to lowest. No empty line inbetween means
        // equal precedence.
        LOGICAL_NOT("!"),
        BITWISE_NOT("~"),
        MINUS("-"),

        MUL("*"),
        DIV("/"),
        MOD("%"),

        PLUS("+"),

        SHIFT_LEFT("<<"),
        SHIFT_RIGHT(">>"),

        LESS_THAN("<"),
        LESS_THAN_EQ("<="),
        GREATER_THAN(">"),
        GREATER_THAN_EQ(">="),

        EQ("=="),
        NOT_EQ("!="),

        BITWISE_AND("&"),

        BITWISE_XOR("^"),

        BITWISE_OR("|"),

        LOGICAL_AND("&&"),

        LOGICAL_OR("||"),

        TERNARY_THEN("?"),
        TERNARY_ELSE(":"),

        ASSIGN("="),
        ASSIGN_PLUS("+="),
        ASSIGN_MINUS("-="),
        ASSIGN_MUL("*="),
        ASSIGN_DIV("/="),
        ASSIGN_MOD("%="),
        ASSIGN_AND("&="),
        ASSIGN_XOR("^="),
        ASSIGN_OR("|="),
        ASSIGN_SHIFT_LEFT("<<="),
        ASSIGN_SHIFT_RIGHT(">>="),;

        public static final List<OperatorType> ASSIGNMENT_OPERATORS = List.of(
            ASSIGN,
            ASSIGN_PLUS,
            ASSIGN_MINUS,
            ASSIGN_MUL,
            ASSIGN_DIV,
            ASSIGN_MOD,
            ASSIGN_AND,
            ASSIGN_XOR,
            ASSIGN_OR,
            ASSIGN_XOR,
            ASSIGN_SHIFT_LEFT,
            ASSIGN_SHIFT_RIGHT
        );

        private final String value;

        OperatorType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }
}
