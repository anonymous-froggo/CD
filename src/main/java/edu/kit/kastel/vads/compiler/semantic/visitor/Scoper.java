package edu.kit.kastel.vads.compiler.semantic.visitor;

import java.util.Stack;

import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.semantic.Namespace;

public abstract class Scoper<T> {

    private final Stack<Namespace<T>> scopes = new Stack<>();
    private Namespace<T> previouslyExitedScope;

    protected Namespace<T> currentScope() {
        // No need to check if this.scopes is empty, it only is in case of a bug
        return this.scopes.peek();
    }

    /// Returns the scope this scoper most recently exited
    protected Namespace<T> previouslyExitedScope() {
        return previouslyExitedScope;
    }

    public void enterNewScope() {
        if (this.scopes.isEmpty()) {
            this.scopes.push(new Namespace<>());
            return;
        }

        duplicateCurrentScope();
    }

    public void exitScope() {
        // No need to check if this.scopes is empty, it only is in case of a bug
        this.previouslyExitedScope = this.scopes.pop();
    }

    private void duplicateCurrentScope() {
        Namespace<T> currentScope = currentScope();
        Namespace<T> clonedScope = new Namespace<T>(currentScope.size());

        for (Name name : currentScope.keySet()) {
            clonedScope.put(name, cloneEntry(currentScope.get(name)));
        }

        this.scopes.push(clonedScope);
    }

    protected abstract T cloneEntry(T t);
}
