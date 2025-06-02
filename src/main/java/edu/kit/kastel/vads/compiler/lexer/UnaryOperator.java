package edu.kit.kastel.vads.compiler.lexer;

import edu.kit.kastel.vads.compiler.Span;

public record UnaryOperator(UnaryOperatorType type, Span span) implements Operator {
    @Override
    public String asString() {
        return type().toString();
    }

    public enum UnaryOperatorType implements OperatorType {
        LOGICAL_NOT("!"),
        BITWISE_NOT("~");
        // unary minus is implemented in {BinaryOperator}

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
