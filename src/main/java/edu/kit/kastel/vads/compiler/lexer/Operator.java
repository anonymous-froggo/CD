package edu.kit.kastel.vads.compiler.lexer;

public sealed interface Operator extends Token permits AssignmentOperator, BinaryOperator, UnaryOperator {
    
    public OperatorType type();

    @Override
    default boolean isOperator(OperatorType operatorType) {
        return operatorType == type();
    }

    @Override
    default String asString() {
        return type().toString();
    }

    public sealed interface OperatorType permits
        AssignmentOperator.AssignmentOperatorType,
        BinaryOperator.BinaryOperatorType,
        UnaryOperator.UnaryOperatorType {
    }

    public enum Associativity {
        LEFT, RIGHT;
    }
}
