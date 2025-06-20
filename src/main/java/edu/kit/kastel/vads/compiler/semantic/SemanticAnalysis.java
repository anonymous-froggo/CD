package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.functions.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.type.Type;
import edu.kit.kastel.vads.compiler.semantic.ret.ReturnAnalysis;
import edu.kit.kastel.vads.compiler.semantic.ret.ReturnState;
import edu.kit.kastel.vads.compiler.semantic.type.TypeAnalysis;
import edu.kit.kastel.vads.compiler.semantic.type.TypeScoper;
import edu.kit.kastel.vads.compiler.semantic.variable.VariableStatusAnalysis;
import edu.kit.kastel.vads.compiler.semantic.variable.VariableStatusScoper;
import edu.kit.kastel.vads.compiler.semantic.visitor.RecursivePostorderVisitor;
import edu.kit.kastel.vads.compiler.semantic.visitor.ScopedRecursivePostorderVisitor;

public class SemanticAnalysis {

    private final ProgramTree program;

    public SemanticAnalysis(ProgramTree program) {
        this.program = program;
    }

    public void analyze() {
        this.program.accept(new RecursivePostorderVisitor<>(new IntegerLiteralRangeAnalysis()), new Namespace<>());

        Namespace<FunctionTree> functions = new Namespace<>();
        this.program.accept(new RecursivePostorderVisitor<>(new FunctionAnalysis()), functions);

        this.program.accept(new ScopedRecursivePostorderVisitor<>(new VariableStatusAnalysis()),
                new VariableStatusScoper());

        this.program.accept(new ScopedRecursivePostorderVisitor<>(new TypeAnalysis(functions)), new TypeScoper());

        this.program.accept(new RecursivePostorderVisitor<>(new ReturnAnalysis()), new ReturnState());
    }
}
