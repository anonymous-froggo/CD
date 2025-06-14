package edu.kit.kastel.vads.compiler.semantic.visitor;

import java.util.Stack;

import edu.kit.kastel.vads.compiler.Visitor;
import edu.kit.kastel.vads.compiler.parser.ast.statements.BlockTree;
import edu.kit.kastel.vads.compiler.semantic.status.Scope;

public class ScopedRecursivePostorderVisitor<R> extends RecursivePostorderVisitor<Scope, R> {

    private final Stack<Scope> scopes = new Stack<>();

    public ScopedRecursivePostorderVisitor(Visitor<Scope, R> visitor) {
        super(visitor);
    }

    private void enterNewScope() {
        if (this.scopes.isEmpty()) {
            this.scopes.push(new Scope());
            return;
        }

        this.scopes.push(currentScope().clone());
    }

    private void exitScope() {
        // No need to check if this.scopes is empty, it only is in case of a bug
        this.scopes.pop();
    }

    public Scope currentScope() {
        // No need to check if this.scopes is empty, it only is in case of a bug
        return this.scopes.peek();
    }

    @Override
    public R visit(BlockTree blockTree, Scope data) {
        enterNewScope();
        R r = super.visit(blockTree, currentScope());
        exitScope();
        return r;
    }
}
