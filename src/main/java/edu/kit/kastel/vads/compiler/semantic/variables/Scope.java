package edu.kit.kastel.vads.compiler.semantic.variables;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.type.Type;
import edu.kit.kastel.vads.compiler.semantic.Namespace;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;
import edu.kit.kastel.vads.compiler.semantic.variables.VariableProperty.Status;

public class Scope extends Namespace<VariableProperty> {

    public Scope() {
        super();
    }

    private Scope(Map<Name, VariableProperty> content) {
        super(content);
    }

    // TODO clone needs to deep-copy [VariableProperty]s, such that
    @Override
    public Scope clone() {
        HashMap<Name, VariableProperty> content = new HashMap<>(super.content.size());

        for (Name name : super.content.keySet()) {
            content.put(name, super.content.get(name).clone());
        }

        return new Scope(content);
    }

    public void declare(NameTree name, Type type) {
        put(name, new VariableProperty(Status.DECLARED, type));
    }

    public void initialize(NameTree name) {
        get(name).setStatus(Status.INITIALIZED);
    }

    public void use(NameTree name) {
        checkInitialized(name);
    }

    public void checkDeclared(NameTree name) {
        Status status = getStatus(name);
        if (status == null) {
            throw new SemanticException("Variable " + name + " must be declared before assignment");
        }
    }

    public void checkInitialized(NameTree name) {
        Status status = getStatus(name);
        if (status == null || status == Status.DECLARED) {
            throw new SemanticException("Variable " + name + " must be initialized before use.");
        }
    }

    public void checkUndeclared(NameTree name) {
        Status status = getStatus(name);
        if (status != null) {
            throw new SemanticException("Variable " + name + " is already declared.");
        }
    }

    public @Nullable Status getStatus(NameTree name) {
        VariableProperty variableProperty = get(name);
        if (variableProperty == null) {
            return null;
        }
        return variableProperty.status();
    }

    public @Nullable Type getType(NameTree name) {
        VariableProperty variableProperty = get(name);
        if (variableProperty == null) {
            return null;
        }
        return variableProperty.type();
    }
}
