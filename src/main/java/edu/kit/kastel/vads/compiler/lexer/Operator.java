package edu.kit.kastel.vads.compiler.lexer;

import edu.kit.kastel.vads.compiler.Span;

public record Operator(OperatorType type, Span span) implements Token {

    @Override
    public boolean isOperator(OperatorType operatorType) {
        return type() == operatorType;
    }

    @Override
    public String asString() {
        return type().toString();
    }

    public enum OperatorType {
        // L1
        ASSIGN_MINUS("-="),
        MINUS("-"),
        ASSIGN_PLUS("+="),
        PLUS("+"),
        MUL("*"),
        ASSIGN_MUL("*="),
        ASSIGN_DIV("/="),
        DIV("/"),
        ASSIGN_MOD("%="),
        MOD("%"),
        ASSIGN("="),

        // L2
        LOGICAL_NOT("!"),
        BITWISE_NOT("~"),
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
        ASSIGN_AND("&="),
        ASSIGN_XOR("^="),
        ASSIGN_OR("|="),
        ASSIGN_SHIFT_LEFT("<<="),
        ASSIGN_SHIFT_RIGHT(">>="),
        ;

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
