package edu.kit.kastel.vads.compiler.semantic.variable;

import java.util.Locale;

public enum VariableStatus {

    DECLARED,
    INITIALIZED;

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
