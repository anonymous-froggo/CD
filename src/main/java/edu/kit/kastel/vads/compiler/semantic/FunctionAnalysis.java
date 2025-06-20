package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.semantic.type.TypeScoper;
import edu.kit.kastel.vads.compiler.semantic.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.semantic.visitor.Unit;
import edu.kit.kastel.vads.compiler.lexer.Ident;
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
        FunctionTree mainFunction = data.get(Name.forIdentString("main"));
        if (mainFunction == null) {
            throw new SemanticException("No main function provided");
        }
        if (mainFunction.returnType().type() != BasicType.INT) {
            throw new SemanticException("main function must return int");
        }

        return NoOpVisitor.super.visit(programTree, data);
    }
}
