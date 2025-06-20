package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.semantic.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.semantic.visitor.Unit;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.functions.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;

public class FunctionAnalysis implements NoOpVisitor<Namespace<FunctionTree>> {

    // Functions

    @Override
    public Unit visit(FunctionTree functionTree, Namespace<FunctionTree> data) {
        if (data.put(functionTree.name(), functionTree) != null) {
            throw new SemanticException(
                    "Duplicate function '" + functionTree.name() + "' at " + functionTree.span());
        }

        return NoOpVisitor.super.visit(functionTree, data);
    }

    // Other trees

    @Override
    public Unit visit(ProgramTree programTree, Namespace<FunctionTree> data) {
        // Check that main is present and returns int
        FunctionTree mainFunction = data.get(Name.forIdentString("main"));
        if (mainFunction == null) {
            throw new SemanticException("No main function provided");
        }
        if (mainFunction.returnType().type() != BasicType.INT) {
            throw new SemanticException("Function 'main' must return int");
        }
        if (!mainFunction.params().isEmpty()) {
            throw new SemanticException("Function 'main' may not have arguments");
        }

        // Add lib functions
        for (FunctionTree libFunction : FunctionTree.LIB_FUNCTIONS) {
            data.put(libFunction.name(), libFunction);
        }

        return NoOpVisitor.super.visit(programTree, data);
    }
}
