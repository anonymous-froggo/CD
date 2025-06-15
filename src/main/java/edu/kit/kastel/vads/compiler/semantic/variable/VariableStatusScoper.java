package edu.kit.kastel.vads.compiler.semantic.variable;

import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.semantic.Namespace;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;
import edu.kit.kastel.vads.compiler.semantic.visitor.Scoper;

public class VariableStatusScoper extends Scoper<VariableStatus> {

    @Override
    protected VariableStatus cloneEntry(VariableStatus t) {
        return t;
    }

    @Override
    public void mergeScopeToCurrent(Namespace<VariableStatus> scope) {
        Namespace<VariableStatus> currentScope = currentScope();

        for (Name name : scope.keySet()) {
            if (scope.get(name) == VariableStatus.INITIALIZED && currentScope.get(name) == VariableStatus.INITIALIZED) {
                // A variable has been initialized in [scope] and not yet in [currentScope].
                currentScope.put(name, VariableStatus.INITIALIZED);
            }
        }
    }

    @Override
    public Namespace<VariableStatus> intersectScopes(
        Namespace<VariableStatus> scope1, Namespace<VariableStatus> scope2
    ) {
        // [scopeIntersection] will contain all variables as initialized which have
        // been initialized in [scope1] and [scope2].
        Namespace<VariableStatus> scopeIntersection = new Namespace<>();

        for (Name name : scope1.keySet()) {
            if (scope1.get(name) == VariableStatus.INITIALIZED && scope2.get(name) == VariableStatus.INITIALIZED) {
                scopeIntersection.put(name, VariableStatus.INITIALIZED);
            }
        }
        
        return scopeIntersection;
    }

    public void declare(NameTree name) {
        currentScope().put(name, VariableStatus.DECLARED);
    }

    public void initialize(NameTree name) {
        currentScope().put(name, VariableStatus.INITIALIZED);
    }

    public void undeclare(NameTree name) {
        currentScope().put(name, null);
    }

    public void checkDeclared(NameTree name) {
        VariableStatus variableStatus = currentScope().get(name);
        if (variableStatus == null) {
            throw new SemanticException("Variable " + name + " must be declared before assignment");
        }
    }

    public void checkInitialized(NameTree name) {
        VariableStatus variableStatus = currentScope().get(name);
        if (variableStatus == null || variableStatus == VariableStatus.DECLARED) {
            throw new SemanticException("Variable " + name + " must be initialized before use.");
        }
    }

    public void checkUndeclared(NameTree name) {
        VariableStatus variableStatus = currentScope().get(name);
        if (variableStatus != null) {
            throw new SemanticException("Variable " + name + " is already declared.");
        }
    }

    public VariableStatus getStatus(NameTree name) {
        return currentScope().get(name);
    }
}
