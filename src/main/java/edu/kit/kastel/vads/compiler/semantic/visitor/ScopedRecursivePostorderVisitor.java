package edu.kit.kastel.vads.compiler.semantic.visitor;

import edu.kit.kastel.vads.compiler.Visitor;
import edu.kit.kastel.vads.compiler.parser.ast.statements.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ElseOptTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.WhileTree;
import edu.kit.kastel.vads.compiler.semantic.Namespace;

public class ScopedRecursivePostorderVisitor<S, T extends Scoper<S>, R> extends RecursivePostorderVisitor<T, R> {

    private boolean nextBlockConditional = false;

    public ScopedRecursivePostorderVisitor(Visitor<T, R> visitor) {
        super(visitor);
    }

    // Enables control flow operations to enter scopes regardless of whether the
    // respective branch has a block statement and avoids a redundant scope being
    // opened.
    private void enterNewConditionalScope(T data) {
        nextBlockConditional = true;
        data.enterNewScope();
    }

    @Override
    public R visit(BlockTree blockTree, T data) {
        boolean exitScopeNeeded;

        if (this.nextBlockConditional) {
            exitScopeNeeded = false;
            nextBlockConditional = false;
        } else {
            exitScopeNeeded = true;
            data.enterNewScope();
        }

        R r;
        T d = data;
        for (StatementTree statement : blockTree.statements()) {
            r = statement.accept(this, d);
            d = accumulate(d, r);
        }

        if (exitScopeNeeded) {
            // Exit scope and merge it to currentScope
            Namespace<S> blockScope = data.exitScope();
            data.mergeScopeToCurrent(blockScope);
        }

        r = this.visitor.visit(blockTree, d);
        return r;
    }

    @Override
    public R visit(ElseOptTree elseOptTree, T data) {
        // Scoping is already managed by visit(IfTree, T). Nothing to be done here.
        return super.visit(elseOptTree, data);
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

        // Encapsule body and step in a conditional scope, but give them separate
        // subscopes
        enterNewConditionalScope(data);

        enterNewConditionalScope(data);
        r = forTree.body().accept(this, accumulate(data, r));
        Namespace<S> bodyScope = data.exitScope();

        // Check step after body (duh)
        if (forTree.step() != null) {
            data.mergeScopeToCurrent(bodyScope);
            data.enterNewScope();
            r = forTree.step().accept(this, accumulate(data, r));
            data.exitScope();
        }

        data.exitScope();

        // Exit the scope in which a variable might be initialized
        // and merge it to the scope above
        Namespace<S> initializerScope = data.exitScope();
        data.mergeScopeToCurrent(initializerScope);

        r = this.visitor.visit(forTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(IfTree ifTree, T data) {
        R r = ifTree.condition().accept(this, data);

        enterNewConditionalScope(data);
        r = ifTree.thenStatement().accept(this, accumulate(data, r));
        Namespace<S> thenScope = data.exitScope();

        if (ifTree.elseOpt() != null) {
            enterNewConditionalScope(data);
            r = ifTree.elseOpt().accept(this, accumulate(data, r));
            Namespace<S> elseScope = data.exitScope();

            // Scopes only need to be merged if there is an else. Otherwise variables cannot
            // be initialized in both control flow branches.
            Namespace<S> ifScope = data.intersectScopes(thenScope, elseScope);
            data.mergeScopeToCurrent(ifScope);
        }

        r = this.visitor.visit(ifTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(WhileTree whileTree, T data) {
        R r = whileTree.condition().accept(this, data);

        enterNewConditionalScope(data);
        r = whileTree.body().accept(this, accumulate(data, r));
        data.exitScope();

        r = this.visitor.visit(whileTree, accumulate(data, r));
        return r;
    }
}
