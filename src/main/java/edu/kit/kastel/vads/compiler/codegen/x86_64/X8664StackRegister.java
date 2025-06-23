package edu.kit.kastel.vads.compiler.codegen.x86_64;

import edu.kit.kastel.vads.compiler.codegen.Register;

public final class X8664StackRegister implements Register {

    public static final int SLOT_SIZE = 8;

    // The stack pointer's offset relative to its position at the start of the corresponding function.
    private static int currentStackPointerOffset;

    private int offsetAtCreation;

    public X8664StackRegister(int slot) {
        this.offsetAtCreation = currentStackPointerOffset + slot * SLOT_SIZE;
    }

    @Override
    public String name(int bitlength) {
        return (offsetAtCreation - currentStackPointerOffset) + "(" + X8664Register.RSP.name(64) + ")";
    }

    @Override
    public String toString() {
        throw new UnsupportedOperationException(
            "toString() used for a register - you must use name(int bitlength) instead"
        );
    }

    public static void resetCurrentStackPointerOffset() {
        currentStackPointerOffset = 0;
    }

    public static void moveStackPointer(int offset) {
        currentStackPointerOffset += offset;
    }
}
