package edu.kit.kastel.vads.compiler.semantic.function;

import edu.kit.kastel.vads.compiler.semantic.Namespace;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;
import edu.kit.kastel.vads.compiler.semantic.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.semantic.visitor.Unit;
import edu.kit.kastel.vads.compiler.parser.ast.functions.FunctionTree;

public class FunctionAnalysis implements NoOpVisitor<Namespace<FunctionTree>> {

    @Override
    public Unit visit(FunctionTree functionTree, Namespace<FunctionTree> data) {
        if (data.put(functionTree.name(), functionTree) != null) {
            throw new SemanticException(
                    "Duplicate function '" + functionTree.name() + "' at " + functionTree.span());
        }

        return NoOpVisitor.super.visit(functionTree, data);
    }
}
