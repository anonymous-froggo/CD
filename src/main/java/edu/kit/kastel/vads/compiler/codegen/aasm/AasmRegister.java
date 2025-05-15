package edu.kit.kastel.vads.compiler.codegen.aasm;

import edu.kit.kastel.vads.compiler.codegen.IRegister;

public record AasmRegister(int id) implements IRegister {
    @Override
    public String toString() {
        return "%" + id();
    }
}
