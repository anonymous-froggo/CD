package edu.kit.kastel.vads.compiler.codegen.x86_64;

import edu.kit.kastel.vads.compiler.codegen.Register;

public final class X8664StackRegister implements Register {

    public static final int SLOT_SIZE_BYTES = 8;

    private String id;

    public X8664StackRegister(X8664Register base, int offset) {
        this.id = offset * SLOT_SIZE_BYTES + "(" + base.name(64) + ")";
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
}
