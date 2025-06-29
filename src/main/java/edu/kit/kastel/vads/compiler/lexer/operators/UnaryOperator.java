package edu.kit.kastel.vads.compiler.lexer.operators;

import edu.kit.kastel.vads.compiler.Span;

public record UnaryOperator(UnaryOperatorType type, Span span) implements Operator {

    public enum UnaryOperatorType implements OperatorType {

        LOGICAL_NOT("!"),
        BITWISE_NOT("~"),
        NEGATE("-");

        private final String value;

        UnaryOperatorType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }
}
