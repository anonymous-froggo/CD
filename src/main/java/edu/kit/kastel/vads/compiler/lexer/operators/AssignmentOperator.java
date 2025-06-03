package edu.kit.kastel.vads.compiler.lexer.operators;

import edu.kit.kastel.vads.compiler.Span;

public record AssignmentOperator(AssignmentOperatorType type, Span span) implements Operator {
    @Override
    public String asString() {
        return type().toString();
    }

    public enum AssignmentOperatorType implements OperatorType {
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
        ASSIGN_SHIFT_RIGHT(">>=");

        private final String value;

        AssignmentOperatorType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }
}
