package edu.kit.kastel.vads.compiler.semantic.variables;

import java.util.Locale;

import edu.kit.kastel.vads.compiler.parser.type.Type;

public class VariableProperty {

    private Status status;
    private Type type;

    public VariableProperty(Status status, Type type) {
        this.status = status;
        this.type = type;
    }

    public Status status() {
        return this.status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Type type() {
        return this.type;
    }

    public enum Status {

        DECLARED,
        INITIALIZED;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
