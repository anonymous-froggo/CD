package edu.kit.kastel.vads.compiler.semantic.visitor;

import edu.kit.kastel.vads.compiler.Visitor;
import edu.kit.kastel.vads.compiler.parser.ast.statements.BlockTree;

public class ScopedRecursivePostorderVisitor<T extends Scoper<?>, R> extends RecursivePostorderVisitor<T, R> {
    public ScopedRecursivePostorderVisitor(Visitor<T, R> visitor) {
        super(visitor);
    }

    @Override
    public R visit(BlockTree blockTree, T data) {
        data.enterNewScope();
        R r = super.visit(blockTree, data);
        data.exitScope();
        return r;
    }
}
