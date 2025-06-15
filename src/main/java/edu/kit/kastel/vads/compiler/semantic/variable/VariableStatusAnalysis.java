package edu.kit.kastel.vads.compiler.semantic.variable;

import edu.kit.kastel.vads.compiler.lexer.operators.AssignmentOperator.AssignmentOperatorType;
import edu.kit.kastel.vads.compiler.parser.ast.LValueIdentifierTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.IdentifierTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ReturnTree;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;
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
    public Unit visit(BreakTree breakTree, VariableStatusScoper data) {
        data.initializeAll();
        return NoOpVisitor.super.visit(breakTree, data);
    }

    @Override
    public Unit visit(ContinueTree continueTree, VariableStatusScoper data) {
        data.initializeAll();
        return NoOpVisitor.super.visit(continueTree, data);
    }

    @Override
    public Unit visit(DeclarationTree declarationTree, VariableStatusScoper data) {
        data.checkUndeclared(declarationTree.name());
        data.declare(declarationTree.name());

        if (declarationTree.initializer() != null) {
            data.initialize(declarationTree.name());
        }

        return NoOpVisitor.super.visit(declarationTree, data);
    }

    @Override
    public Unit visit(ForTree forTree, VariableStatusScoper data) {
        // Check that forTree.postBody() is not a declaration.
        // Don't really have a better place for that than here.
        if (forTree.postBody() instanceof DeclarationTree) {
            throw new SemanticException("The step statement in a for loop may not be a declaration");
        }
        return NoOpVisitor.super.visit(forTree, data);
    }

    @Override
    public Unit visit(ReturnTree returnTree, VariableStatusScoper data) {
        data.initializeAll();
        return NoOpVisitor.super.visit(returnTree, data);
    }
}
