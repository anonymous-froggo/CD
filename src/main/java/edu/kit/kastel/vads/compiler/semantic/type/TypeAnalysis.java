package edu.kit.kastel.vads.compiler.semantic.type;

import edu.kit.kastel.vads.compiler.lexer.operators.AssignmentOperator.AssignmentOperatorType;
import edu.kit.kastel.vads.compiler.parser.ast.LValueIdentifierTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.BoolTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.IdentifierTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.NumberLiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.TernaryTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.UnaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.WhileTree;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;
import edu.kit.kastel.vads.compiler.parser.type.Type;
import edu.kit.kastel.vads.compiler.semantic.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.semantic.visitor.Unit;

public class TypeAnalysis implements NoOpVisitor<TypeScoper> {

    // Expression trees

    @Override
    public Unit visit(BinaryOperationTree binaryOperationTree, TypeScoper data) {
        ExpressionTree lhs = binaryOperationTree.lhs();
        ExpressionTree rhs = binaryOperationTree.rhs();

        switch (binaryOperationTree.operator().type()) {
            case LESS_THAN, LESS_THAN_EQ, GREATER_THAN, GREATER_THAN_EQ -> {
                data.checkTypesMatch(BasicType.INT, lhs, rhs);
                data.setType(binaryOperationTree, BasicType.BOOL);
            }
            case EQ, NOT_EQ -> {
                data.checkTypesEqual(lhs, rhs);
                data.setType(binaryOperationTree, BasicType.BOOL);
            }
            case LOGICAL_AND, LOGICAL_OR -> {
                data.checkTypesMatch(BasicType.BOOL, lhs, rhs);
                data.setType(binaryOperationTree, BasicType.BOOL);
            }
            default -> {
                data.checkTypesMatch(BasicType.INT, lhs, rhs);
                data.setType(binaryOperationTree, BasicType.INT);
            }
        }

        return NoOpVisitor.super.visit(binaryOperationTree, data);
    }

    @Override
    public Unit visit(BoolTree trueTree, TypeScoper data) {
        data.setType(trueTree, BasicType.BOOL);
        return NoOpVisitor.super.visit(trueTree, data);
    }

    @Override
    public Unit visit(IdentifierTree identifierTree, TypeScoper data) {
        data.setType(identifierTree, data.getType(identifierTree.name()));
        return NoOpVisitor.super.visit(identifierTree, data);
    }

    @Override
    public Unit visit(NumberLiteralTree literalTree, TypeScoper data) {
        data.setType(literalTree, BasicType.INT);
        return NoOpVisitor.super.visit(literalTree, data);
    }

    @Override
    public Unit visit(TernaryTree ternaryTree, TypeScoper data) {
        data.checkTypesMatch(BasicType.BOOL, ternaryTree.condition());
        Type type = data.checkTypesEqual(ternaryTree.thenExpression(), ternaryTree.elseExpression());
        data.setType(ternaryTree, type);
        return NoOpVisitor.super.visit(ternaryTree, data);
    }

    @Override
    public Unit visit(UnaryOperationTree unaryOperationTree, TypeScoper data) {
        ExpressionTree operand = unaryOperationTree.operand();

        switch (unaryOperationTree.operator().type()) {
            case LOGICAL_NOT -> {
                data.checkTypesMatch(BasicType.BOOL, operand);
                data.setType(unaryOperationTree, BasicType.BOOL);
            }
            default -> {
                data.checkTypesMatch(BasicType.INT, operand);
                data.setType(unaryOperationTree, BasicType.INT);
            }
        }

        return NoOpVisitor.super.visit(unaryOperationTree, data);
    }

    // Statement trees

    @Override
    public Unit visit(AssignmentTree assignmentTree, TypeScoper data) {
        switch (assignmentTree.lValue()) {
            case LValueIdentifierTree(var name) -> {
                if (assignmentTree.operatorType() == AssignmentOperatorType.ASSIGN) {
                    data.checkTypesEqual(name, assignmentTree.expression());
                } else {
                    data.checkTypesMatch(BasicType.INT, name, assignmentTree.expression());
                }
            }
        }

        return NoOpVisitor.super.visit(assignmentTree, data);
    }

    @Override
    public Unit visit(DeclarationTree declarationTree, TypeScoper data) {
        data.setType(declarationTree.name(), declarationTree.type().type());

        if (declarationTree.initializer() != null) {
            data.checkTypesEqual(declarationTree.name(), declarationTree.initializer());
        }

        return NoOpVisitor.super.visit(declarationTree, data);
    }

    @Override
    public Unit visit(ForTree forTree, TypeScoper data) {
        data.checkTypesMatch(BasicType.BOOL, forTree.condition());

        return NoOpVisitor.super.visit(forTree, data);
    }

    @Override
    public Unit visit(IfTree ifTree, TypeScoper data) {
        data.checkTypesMatch(BasicType.BOOL, ifTree.condition());
        return NoOpVisitor.super.visit(ifTree, data);
    }

    @Override
    public Unit visit(ReturnTree returnTree, TypeScoper data) {
        data.checkTypesMatch(BasicType.INT, returnTree.expression());
        return NoOpVisitor.super.visit(returnTree, data);
    }

    @Override
    public Unit visit(WhileTree whileTree, TypeScoper data) {
        data.checkTypesMatch(BasicType.BOOL, whileTree.condition());
        return NoOpVisitor.super.visit(whileTree, data);
    }
}
