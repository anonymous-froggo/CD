package edu.kit.kastel.vads.compiler.codegen.x86_64;

import edu.kit.kastel.vads.compiler.codegen.Register;

public final class X8664StackRegister implements Register {

    public static final int N_BYTES = 8;

    private int offset;
    private String id;

    // TODO maybe enable stack register sizes other than 64 bit
    public X8664StackRegister(int offset) {
        this.offset = offset;
        this.id = this.offset + "(" + X8664Register.RSP.name(N_BYTES * 8) + ")";
    }

    @Override
    public String name(int bitlength) {
        return this.id;
    }

    @Override
    public String toString() {
        throw new UnsupportedOperationException(
            "toString() used for a register - you must use name(int bitlength) instead"
        );
    }

    public int getOffset() {
        return this.offset;
    }
}
