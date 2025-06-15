package edu.kit.kastel.vads.compiler.semantic.variable;

import edu.kit.kastel.vads.compiler.lexer.operators.AssignmentOperator.AssignmentOperatorType;
import edu.kit.kastel.vads.compiler.parser.ast.LValueIdentifierTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.IdentifierTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.DeclarationTree;
import edu.kit.kastel.vads.compiler.semantic.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.semantic.visitor.Unit;

public class VariableStatusAnalysis implements NoOpVisitor<VariableStatusScoper> {

    // Expression trees

    @Override
    public Unit visit(IdentifierTree identExpressionTree, VariableStatusScoper data) {
        data.checkInitialized(identExpressionTree.name());
        return NoOpVisitor.super.visit(identExpressionTree, data);
    }

    // Statement trees

    @Override
    public Unit visit(AssignmentTree assignmentTree, VariableStatusScoper data) {
        switch (assignmentTree.lValue()) {
            case LValueIdentifierTree(var name) -> {
                if (assignmentTree.operatorType() == AssignmentOperatorType.ASSIGN) {
                    data.checkDeclared(name);
                } else {
                    data.checkInitialized(name);
                }

                if (data.getStatus(name) != VariableStatus.INITIALIZED) {
                    data.initialize(name);
                }
            }
        }
        return NoOpVisitor.super.visit(assignmentTree, data);
    }

    @Override
    public Unit visit(DeclarationTree declarationTree, VariableStatusScoper data) {
        data.checkUndeclared(declarationTree.name());
        return NoOpVisitor.super.visit(declarationTree, data);
    }
}
