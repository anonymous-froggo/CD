package edu.kit.kastel.vads.compiler.codegen.x86_64;

import edu.kit.kastel.vads.compiler.codegen.IRegister;

public enum X8664Register implements IRegister {

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

    String name64;
    String name32;
    String name16;
    String name8;

    private static final IRegister[] GENERAL_PURPOSE_REGISTERS = new IRegister[] {
        R8, R9, R10, R11, R12, R13, R14
    };

    private X8664Register(String name64, String name32, String name16, String name8) {
        this.name64 = "%" + name64;
        this.name32 = "%" + name32;
        this.name16 = "%" + name16;
        this.name8 = "%" + name8;
    }

    public String name64() {
        return this.name64;
    }

    public String name32() {
        return this.name32;
    }

    public String name16() {
        return this.name16;
    }

    public String name8() {
        return this.name8;
    }

    public static IRegister[] getGeneralPurposeRegisters() {
        return GENERAL_PURPOSE_REGISTERS.clone();
    }
}
