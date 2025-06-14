package edu.kit.kastel.vads.compiler.semantic.variables;

import edu.kit.kastel.vads.compiler.lexer.operators.AssignmentOperator.AssignmentOperatorType;
import edu.kit.kastel.vads.compiler.parser.ast.LValueIdentifierTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.IdentifierTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.NumberLiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ElseOptTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.IfTree;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;
import edu.kit.kastel.vads.compiler.parser.type.Type;
import edu.kit.kastel.vads.compiler.semantic.Namespace;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;
import edu.kit.kastel.vads.compiler.semantic.variables.VariableProperty.Status;
import edu.kit.kastel.vads.compiler.semantic.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.semantic.visitor.Unit;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

// TODO note additional properties that are checked
// TODO change name -> should also include type checking
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

    private void setInferredType(ExpressionTree expression, Type type) {
        this.inferredTypes.put(expression, type);
    }

    private void checkTypesEqual(NameTree name, ExpressionTree expression, Scope scope) {
        Type variableType = scope.getType(name);
        Type expressionType = getInferredType(expression);
        if (variableType != expressionType) {
            throw new SemanticException(
                "Type mismatch: cannot convert from " + expressionType + " to " + variableType
            );
        }
    }

    // Expression trees

    @Override
    public Unit visit(IdentifierTree identifierTree, Scope data) {
        data.checkInitialized(identifierTree.name());

        setInferredType(identifierTree, data.getType(identifierTree.name()));

        return NoOpVisitor.super.visit(identifierTree, data);
    }

    @Override
    public Unit visit(NumberLiteralTree literalTree, Scope data) {
        setInferredType(literalTree, BasicType.INT);
        
        return NoOpVisitor.super.visit(literalTree, data);
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
                
                checkTypesEqual(name, assignmentTree.expression(), data);

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
            checkTypesEqual(name, initializer, data);
            data.initialize(name);
        }

        return NoOpVisitor.super.visit(declarationTree, data);
    }
}
