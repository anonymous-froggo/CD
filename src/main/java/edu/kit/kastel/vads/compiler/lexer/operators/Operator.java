package edu.kit.kastel.vads.compiler.lexer.operators;

import edu.kit.kastel.vads.compiler.lexer.Token;
import edu.kit.kastel.vads.compiler.lexer.operators.AssignmentOperator.AssignmentOperatorType;
import edu.kit.kastel.vads.compiler.lexer.operators.BinaryOperator.BinaryOperatorType;
import edu.kit.kastel.vads.compiler.lexer.operators.UnaryOperator.UnaryOperatorType;

public sealed interface Operator extends Token permits AssignmentOperator, BinaryOperator, UnaryOperator {

    OperatorType type();

    @Override
    default boolean isOperator(OperatorType operatorType) {
        return operatorType == type();
    }

    @Override
    default String asString() {
        return type().toString();
    }

    sealed interface OperatorType permits AssignmentOperatorType, BinaryOperatorType, UnaryOperatorType {
    }

    enum Associativity {

        LEFT, RIGHT;
    }
}
