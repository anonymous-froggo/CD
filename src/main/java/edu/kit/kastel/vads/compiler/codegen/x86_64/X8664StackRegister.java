package edu.kit.kastel.vads.compiler.codegen.x86_64;

import edu.kit.kastel.vads.compiler.codegen.IRegister;

public class X8664StackRegister implements IRegister {

    private int offset;
    private String id;

    public X8664StackRegister(int offset) {
        this.offset = offset;
        this.id = this.offset + "(" + X8664Register.RSP + ")";
    }

    @Override
    public String toString() {
        return this.id;
    }

    public int getOffset() {
        return this.offset;
    }
}
