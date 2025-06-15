package edu.kit.kastel.vads.compiler.semantic.visitor;

import edu.kit.kastel.vads.compiler.Visitor;
import edu.kit.kastel.vads.compiler.parser.ast.statements.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ForTree;

public class ScopedRecursivePostorderVisitor<S, T extends Scoper<S>, R> extends RecursivePostorderVisitor<T, R> {

    private boolean ignoreBlock = false;

    public ScopedRecursivePostorderVisitor(Visitor<T, R> visitor) {
        super(visitor);
    }

    // Enables control flow operations to enter scopes regardless of whether the
    // respective branch has a block statement and avoids a redundant scope being
    // opened.
    private void enterNewScopeIgnoreBlock(T data) {
        ignoreBlock = true;
        data.enterNewScope();
    }

    @Override
    public R visit(BlockTree blockTree, T data) {
        boolean exitScopeNeeded;

        if (this.ignoreBlock) {
            exitScopeNeeded = false;
            ignoreBlock = false;
        } else {
            exitScopeNeeded = true;
            data.enterNewScope();
        }

        R r = super.visit(blockTree, data);

        if (exitScopeNeeded) {
            data.exitScope();
        }

        return r;
    }

    @Override
    public R visit(ForTree forTree, T data) {
        // Enter the scope in which a variable might be initialized
        // in forTree.initializer()
        data.enterNewScope();

        R r = null;
        if (forTree.initializer() != null) {
            r = forTree.initializer().accept(this, data);
        }
        r = forTree.condition().accept(this, accumulate(data, r));
        if (forTree.postBody() != null) {
            r = forTree.postBody().accept(this, accumulate(data, r));
        }

        // Don't look inside loop body, encapsule it in a new scope
        enterNewScopeIgnoreBlock(data);
        r = forTree.body().accept(this, accumulate(data, r));
        data.exitScope();

        // Exit the scope in which a variable might be initialized
        // and merge it to the scope above
        data.exitScope();
        data.mergeScopeToCurrent(data.previouslyExitedScope());

        r = this.visitor.visit(forTree, accumulate(data, r));
        return r;
    }
}
