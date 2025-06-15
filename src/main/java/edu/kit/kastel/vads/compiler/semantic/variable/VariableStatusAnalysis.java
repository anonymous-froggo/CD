package edu.kit.kastel.vads.compiler.semantic.variable;

import edu.kit.kastel.vads.compiler.parser.ast.statements.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.DeclarationTree;
import edu.kit.kastel.vads.compiler.semantic.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.semantic.visitor.Unit;

public class VariableStatusAnalysis implements NoOpVisitor<VariableStatusScoper> {
    @Override
    public Unit visit(DeclarationTree declarationTree, VariableStatusScoper data) {
        data.checkUndeclared(declarationTree.name());
        return NoOpVisitor.super.visit(declarationTree, data);
    }
}
