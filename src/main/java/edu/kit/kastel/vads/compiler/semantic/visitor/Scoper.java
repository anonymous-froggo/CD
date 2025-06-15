package edu.kit.kastel.vads.compiler.semantic.visitor;

import java.util.Stack;

import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.semantic.Namespace;

public abstract class Scoper<S> {

    private final Stack<Namespace<S>> scopes = new Stack<>();

    protected Namespace<S> currentScope() {
        // No need to check if this.scopes is empty, it only is in case of a bug
        return this.scopes.peek();
    }

    protected boolean inProgramScope() {
        return this.scopes.isEmpty();
    }

    public void enterNewScope() {
        if (inProgramScope()) {
            this.scopes.push(new Namespace<>());
            return;
        }

        duplicateCurrentScope();
    }

    public Namespace<S> exitScope() {
        // No need to check if this.scopes is empty, it only is in case of a bug
        return this.scopes.pop();
    }

    private void duplicateCurrentScope() {
        Namespace<S> currentScope = currentScope();
        Namespace<S> clonedScope = new Namespace<S>(currentScope.size());

        for (Name name : currentScope.keySet()) {
            clonedScope.put(name, cloneEntry(currentScope.get(name)));
        }

        this.scopes.push(clonedScope);
    }

    protected abstract S cloneEntry(S t);

    public abstract void mergeScopeToCurrent(Namespace<S> scope);

    public abstract Namespace<S> intersectScopes(Namespace<S> scope1, Namespace<S> scope2);
}
