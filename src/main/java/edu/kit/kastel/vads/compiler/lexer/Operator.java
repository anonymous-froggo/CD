package edu.kit.kastel.vads.compiler.lexer;

import java.util.List;

import edu.kit.kastel.vads.compiler.Span;

public sealed interface Operator extends Token permits UnaryOperator, BinaryOperator, AssignmentOperator {
    public OperatorType type();

    @Override
    default boolean isOperator(OperatorType operatorType) {
        return operatorType == type();
    }

    public interface OperatorType {
        public int getPrecedence();

        public Associativity getAssociativity();
    }

    public enum Associativity {
        LEFT, RIGHT;
    }
}
