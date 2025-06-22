package edu.kit.kastel.vads.compiler.codegen.x86_64;

import edu.kit.kastel.vads.compiler.codegen.Register;

public enum X8664Register implements Register {

    RAX("rax", "eax", "ax", "al"),
    RBX("rbx", "ebx", "bx", "bl"),
    RCX("rcx", "ecx", "cx", "cl"),
    RDX("rdx", "edx", "dx", "dl"),

    RSI("rsi", "esi", "si", "sil"),
    RDI("rdi", "edi", "di", "dil"),
    RSP("rsp", "esp", "sp", "spl"),
    RBP("rbp", "ebp", "bp", "bpl"),

    R8("r8", "r8d", "r8w", "r8b"),
    R9("r9", "r9d", "r9w", "r9b"),
    R10("r10", "r10d", "r10w", "r10b"),
    R11("r11", "r11d", "r11w", "r11b"),
    R12("r12", "r12d", "r12w", "r12b"),
    R13("r13", "r13d", "r13w", "r13b"),
    R14("r14", "r14d", "r14w", "r14b"),
    R15("r15", "r15d", "r15w", "r15b");

    private String name64;
    private String name32;
    private String name16;
    private String name8;

    private static final Register[] GENERAL_PURPOSE_REGISTERS = new Register[] {
        R8, R9, R10, R11, R12, R13, R14, R15,
    };
    private static final Register[] PARAM_REGISTERS = new Register[] {
        RDI, RSI, RDX, RCX, R8, R9,
    };
    private static final Register[] CALLEE_SAVED = new Register[] {
        RBX, RSP, RBP, R12, R13, R14, R15,
    };
    // RAX would be caller saved, but as it contains the result of a function call,
    // we don't actually save/load it
    private static final Register[] CALLER_SAVED = new Register[] {
        /*RAX, */RCX, RDX, RSI, RDI, R8, R9, R10, R11,
    };

    private X8664Register(String name64, String name32, String name16, String name8) {
        this.name64 = "%" + name64;
        this.name32 = "%" + name32;
        this.name16 = "%" + name16;
        this.name8 = "%" + name8;
    }

    @Override
    public String name(int bitlength) {
        return switch (bitlength) {
            case (64) -> this.name64;
            case (32) -> this.name32;
            case (16) -> this.name16;
            case (8) -> this.name8;
            default -> throw new IllegalArgumentException(
                "bitlength " + bitlength + " not applicable for " + X8664Register.class
            );
        };
    }

    @Override
    public String toString() {
        throw new UnsupportedOperationException(
            "toString() used for a register - you must use name(int bitlength) instead"
        );
    }

    public static Register[] generalPurposeRegisters() {
        return GENERAL_PURPOSE_REGISTERS.clone();
    }

    public static Register[] paramRegisters() {
        return PARAM_REGISTERS.clone();
    }
}
