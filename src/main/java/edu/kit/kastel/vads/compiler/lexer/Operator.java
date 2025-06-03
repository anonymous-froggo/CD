package edu.kit.kastel.vads.compiler.lexer;

import edu.kit.kastel.vads.compiler.lexer.AssignmentOperator.AssignmentOperatorType;
import edu.kit.kastel.vads.compiler.lexer.BinaryOperator.BinaryOperatorType;
import edu.kit.kastel.vads.compiler.lexer.UnaryOperator.UnaryOperatorType;

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

    public sealed interface OperatorType permits AssignmentOperatorType, BinaryOperatorType, UnaryOperatorType {
    }

    public enum Associativity {
        
        LEFT, RIGHT;
    }
}
