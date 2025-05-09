package edu.kit.kastel.vads.compiler.regalloc.aasm;

import edu.kit.kastel.vads.compiler.regalloc.IRegister;

public record AasmRegister(int id) implements IRegister {
    @Override
    public String toString() {
        return "%" + id();
    }
}
