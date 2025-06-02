package edu.kit.kastel.vads.compiler.lexer;

import edu.kit.kastel.vads.compiler.Span;

public record BinaryOperator(BinaryOperatorType type, Span span) implements Operator {
    @Override
    public String asString() {
        return type().toString();
    }

    public enum BinaryOperatorType implements OperatorType {
        MUL("*", 11),
            DIV("/", 11),
        MOD("%", 11),

        PLUS("+", 10),
        MINUS("-", 10), // may also be a unary minus if it is at the beginning of an atom

        SHIFT_LEFT("<<", 9),
        SHIFT_RIGHT(">>", 9),

        LESS_THAN("<", 8),
        LESS_THAN_EQ("<=", 8),
        GREATER_THAN(">", 8),
        GREATER_THAN_EQ(">=", 8),

        EQ("==", 7),
        NOT_EQ("!=", 7),

        BITWISE_AND("&", 6),

        BITWISE_XOR("^", 5),

        BITWISE_OR("|", 4),

        LOGICAL_AND("&&", 3),

        LOGICAL_OR("||", 2);
        
        private final String value;
        private final int precedence;
        private final Associativity associativity;

        BinaryOperatorType(String value, int precedence) {
            this.value = value;
            this.precedence = precedence;
            this.associativity = Associativity.LEFT;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public int getPrecedence() {
            return this.precedence;
        }

        public Associativity getAssociativity() {
            return this.associativity;
        }
    }
}
