package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.lexer.Operator;
import edu.kit.kastel.vads.compiler.lexer.BinaryOperator.BinaryOperatorType;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record BinaryOperationTree(
    ExpressionTree lhs, ExpressionTree rhs, BinaryOperatorType operatorType) implements ExpressionTree {
    @Override
    public Span span() {
        return lhs().span().merge(rhs().span());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
