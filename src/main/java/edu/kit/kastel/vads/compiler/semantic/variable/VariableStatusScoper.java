package edu.kit.kastel.vads.compiler.semantic.variable;

import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.type.Type;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;
import edu.kit.kastel.vads.compiler.semantic.visitor.Scoper;

public class VariableStatusScoper extends Scoper<VariableStatus> {

    @Override
    protected VariableStatus cloneEntry(VariableStatus t) {
        return t;
    }

    public void declare(NameTree name) {
        currentScope().put(name, VariableStatus.DECLARED);
    }

    public void initialize(NameTree name) {
        currentScope().put(name, VariableStatus.INITIALIZED);
    }

    // public void undeclare(NameTree name) {
    //     currentScope().put(name, null);
    // }

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
