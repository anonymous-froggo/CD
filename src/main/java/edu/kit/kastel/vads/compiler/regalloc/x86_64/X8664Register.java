package edu.kit.kastel.vads.compiler.regalloc.x86_64;

import edu.kit.kastel.vads.compiler.regalloc.IRegister;

public record X8664Register(String id) implements IRegister {
    public static final IRegister RAX = new X8664Register("rax");
    public static final IRegister RBX = new X8664Register("rbx");
    public static final IRegister RCX = new X8664Register("rcx");
    public static final IRegister RDX = new X8664Register("rdx");
    public static final IRegister RSI = new X8664Register("rsi");
    public static final IRegister RDI = new X8664Register("rdi");
    public static final IRegister RSP = new X8664Register("rsp");
    public static final IRegister RBP = new X8664Register("rbp");
    public static final IRegister R8 = new X8664Register("r8");
    public static final IRegister R9 = new X8664Register("r9");
    public static final IRegister R10 = new X8664Register("r10");
    public static final IRegister R11 = new X8664Register("r11");
    public static final IRegister R12 = new X8664Register("r12");
    public static final IRegister R13 = new X8664Register("r13");
    public static final IRegister R14 = new X8664Register("r14");
    public static final IRegister R15 = new X8664Register("r15");

    
    private static final IRegister[] GENERAL_PURPOSE_REGISTERS = new IRegister[] { R8, R9, R10, R11, R12, R13, R14, R15 };

    @Override
    public String toString() {
        return "%" + this.id;
    }

    public static IRegister[] getGeneralPurposeRegisters() {
        return GENERAL_PURPOSE_REGISTERS.clone();
    }
}
