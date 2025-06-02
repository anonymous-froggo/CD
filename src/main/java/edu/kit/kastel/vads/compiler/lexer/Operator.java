package edu.kit.kastel.vads.compiler.lexer;

public sealed interface Operator extends Token permits UnaryOperator, BinaryOperator, AssignmentOperator {
    public OperatorType type();

    @Override
    default boolean isOperator(OperatorType operatorType) {
        return operatorType == type();
    }

    public interface OperatorType {
    }

    public enum Associativity {
        LEFT, RIGHT;
    }
}
