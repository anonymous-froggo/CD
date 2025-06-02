package edu.kit.kastel.vads.compiler.lexer;

import edu.kit.kastel.vads.compiler.Span;

public record UnaryOperator(UnaryOperatorType type, Span span) implements Operator {
    @Override
    public String asString() {
        return type().toString();
    }

    public enum UnaryOperatorType implements OperatorType {
        LOGICAL_NOT("!", 12),
        BITWISE_NOT("~", 12),
        UNARY_MINUS("-", 12);

        private final String value;
        private final int precedence;
        private final Associativity associativity;

        UnaryOperatorType(String value, int precedence) {
            this.value = value;
            this.precedence = precedence;
            this.associativity = Associativity.RIGHT;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @Override
        public int getPrecedence() {
            return this.precedence;
        }

        @Override
        public Associativity getAssociativity() {
            return this.associativity;
        }
    }
}
