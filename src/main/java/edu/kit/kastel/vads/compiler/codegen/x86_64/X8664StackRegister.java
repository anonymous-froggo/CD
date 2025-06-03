package edu.kit.kastel.vads.compiler.codegen.x86_64;

import edu.kit.kastel.vads.compiler.codegen.IRegister;

public final class X8664StackRegister implements IRegister {

    private int offset;
    private String id;

    public X8664StackRegister(int offset) {
        this.offset = offset;
        this.id = this.offset + "(" + X8664Register.RSP + ")";
    }

    @Override
    public String name(int bitlength) {
        return this.id;
    }
    
    @Override
    public String toString() {
        throw new UnsupportedOperationException(
            "toString() used for a register. You should use name(int bitlength) instead"
        );
    }

    public int getOffset() {
        return this.offset;
    }
}
