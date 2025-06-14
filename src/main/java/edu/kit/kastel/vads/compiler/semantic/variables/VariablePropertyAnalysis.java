package edu.kit.kastel.vads.compiler.semantic.variables;

import edu.kit.kastel.vads.compiler.lexer.operators.AssignmentOperator.AssignmentOperatorType;
import edu.kit.kastel.vads.compiler.parser.ast.LValueIdentifierTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
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
import edu.kit.kastel.vads.compiler.semantic.SemanticException;
import edu.kit.kastel.vads.compiler.semantic.variables.VariableProperty.Status;
import edu.kit.kastel.vads.compiler.semantic.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.semantic.visitor.Unit;

import java.util.HashMap;
import java.util.Map;

// TODO note additional properties that are checked
// TODO change name -> should also include type checking
// TODO maybe move expression type inference to separate visitor
/// Checks that variables are
/// - declared before assignment
/// - not declared twice
/// - not initialized twice
/// - assigned before referenced
public class VariablePropertyAnalysis implements NoOpVisitor<Scope> {

    Map<ExpressionTree, Type> inferredTypes = new HashMap<>();

    private Type getInferredType(ExpressionTree expression) {
        return this.inferredTypes.get(expression);
    }

    private void addInferredType(ExpressionTree expression, Type type) {
        this.inferredTypes.put(expression, type);
    }

    private Type checkTypesEqual(Scope scope, NameTree name, ExpressionTree expression) {
        Type variableType = scope.getType(name);
        Type expressionType = getInferredType(expression);
        if (variableType != expressionType) {
            throw new SemanticException(
                "Type mismatch: cannot convert from " + expressionType + " to " + variableType
            );
        }

        return variableType;
    }

    private Type checkTypesEqual(ExpressionTree... expressions) {
        Type prevType = null;
        for (ExpressionTree expression : expressions) {
            Type type = getInferredType(expression);
            if (prevType != null && prevType != type) {
                throw new SemanticException(
                    "Type mismatch: cannot convert from " + prevType + " to " + type
                );
            }
            prevType = type;
        }

        return prevType;
    }

    private void checkTypesMatch(Type type, ExpressionTree... expressions) {
        for (ExpressionTree expression : expressions) {
            Type expressionType = getInferredType(expression);
            if (expressionType != type) {
                throw new SemanticException(
                    "Type mismatch: cannot convert from " + expressionType + " to " + type
                );
            }
        }
    }

    // Expression trees

    @Override
    public Unit visit(BinaryOperationTree binaryOperationTree, Scope data) {
        ExpressionTree lhs = binaryOperationTree.lhs();
        ExpressionTree rhs = binaryOperationTree.rhs();

        switch (binaryOperationTree.operator().type()) {
            case LESS_THAN, LESS_THAN_EQ, GREATER_THAN, GREATER_THAN_EQ -> {
                checkTypesMatch(BasicType.INT, lhs, rhs);
                addInferredType(binaryOperationTree, BasicType.BOOL);
            }
            case EQ, NOT_EQ -> {
                checkTypesEqual(lhs, rhs);
                addInferredType(binaryOperationTree, BasicType.BOOL);
            }
            case LOGICAL_AND, LOGICAL_OR -> {
                checkTypesMatch(BasicType.BOOL, lhs, rhs);
                addInferredType(binaryOperationTree, BasicType.BOOL);
            }
            default -> {
                checkTypesMatch(BasicType.INT, lhs, rhs);
                addInferredType(binaryOperationTree, BasicType.INT);
            }
        }

        return NoOpVisitor.super.visit(binaryOperationTree, data);
    }

    @Override
    public Unit visit(BoolTree trueTree, Scope data) {
        addInferredType(trueTree, BasicType.BOOL);
        return NoOpVisitor.super.visit(trueTree, data);
    }

    @Override
    public Unit visit(IdentifierTree identifierTree, Scope data) {
        data.checkInitialized(identifierTree.name());

        addInferredType(identifierTree, data.getType(identifierTree.name()));
        return NoOpVisitor.super.visit(identifierTree, data);
    }

    @Override
    public Unit visit(NumberLiteralTree literalTree, Scope data) {
        addInferredType(literalTree, BasicType.INT);
        return NoOpVisitor.super.visit(literalTree, data);
    }

    @Override
    public Unit visit(TernaryTree ternaryTree, Scope data) {
        checkTypesMatch(BasicType.BOOL, ternaryTree.condition());
        Type type = checkTypesEqual(ternaryTree.thenExpression(), ternaryTree.elseExpression());
        addInferredType(ternaryTree, type);
        return NoOpVisitor.super.visit(ternaryTree, data);
    }

    @Override
    public Unit visit(UnaryOperationTree unaryOperationTree, Scope data) {
        ExpressionTree operand = unaryOperationTree.operand();

        switch (unaryOperationTree.operator().type()) {
            case LOGICAL_NOT -> {
                checkTypesMatch(BasicType.BOOL, operand);
                addInferredType(unaryOperationTree, BasicType.BOOL);
            }
            default -> {
                checkTypesMatch(BasicType.INT, operand);
                addInferredType(unaryOperationTree, BasicType.INT);
            }
        }

        return NoOpVisitor.super.visit(unaryOperationTree, data);
    }

    // Statement trees

    @Override
    public Unit visit(AssignmentTree assignmentTree, Scope data) {
        switch (assignmentTree.lValue()) {
            case LValueIdentifierTree(var name) -> {
                if (assignmentTree.operatorType() == AssignmentOperatorType.ASSIGN) {
                    data.checkDeclared(name);
                } else {
                    data.checkInitialized(name);
                }

                checkTypesEqual(data, name, assignmentTree.expression());

                if (data.getStatus(name) != Status.INITIALIZED) {
                    // only update when needed, reassignment is totally fine
                    data.initialize(name);
                }
            }
        }

        return NoOpVisitor.super.visit(assignmentTree, data);
    }

    @Override
    public Unit visit(DeclarationTree declarationTree, Scope data) {
        NameTree name = declarationTree.name();
        Type type = declarationTree.type().type();
        ExpressionTree initializer = declarationTree.initializer();

        data.checkUndeclared(name);
        data.declare(name, type);

        if (initializer != null) {
            checkTypesEqual(data, name, initializer);
            data.initialize(name);
        }

        return NoOpVisitor.super.visit(declarationTree, data);
    }

    @Override
    public Unit visit(ForTree forTree, Scope data) {
        if (forTree.postBody() instanceof DeclarationTree) {
            throw new SemanticException("The step statement in a for loop may not be a declaration");
        }
        checkTypesMatch(BasicType.BOOL, forTree.condition());
        return NoOpVisitor.super.visit(forTree, data);
    }

    @Override
    public Unit visit(IfTree ifTree, Scope data) {
        checkTypesMatch(BasicType.BOOL, ifTree.condition());
        return NoOpVisitor.super.visit(ifTree, data);
    }

    @Override
    public Unit visit(ReturnTree returnTree, Scope data) {
        checkTypesMatch(BasicType.INT, returnTree.expression());
        return NoOpVisitor.super.visit(returnTree, data);
    }

    @Override
    public Unit visit(WhileTree whileTree, Scope data) {
        checkTypesMatch(BasicType.BOOL, whileTree.condition());
        return NoOpVisitor.super.visit(whileTree, data);
    }
}
