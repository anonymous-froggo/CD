package edu.kit.kastel.vads.compiler.codegen.x86_64;

import edu.kit.kastel.vads.compiler.codegen.IRegister;

public record X8664Register(String id) implements IRegister {
    public static final IRegister RAX = new X8664Register("eax");
    public static final IRegister RBX = new X8664Register("ebx");
    public static final IRegister RCX = new X8664Register("ecx");
    public static final IRegister RDX = new X8664Register("edx");
    public static final IRegister RSI = new X8664Register("esi");
    public static final IRegister RDI = new X8664Register("edi");
    public static final IRegister RSP = new X8664Register("esp");
    public static final IRegister RBP = new X8664Register("ebp");
    public static final IRegister R8 = new X8664Register("r8d");
    public static final IRegister R9 = new X8664Register("r9d");
    public static final IRegister R10 = new X8664Register("r10d");
    public static final IRegister R11 = new X8664Register("r11d");
    public static final IRegister R12 = new X8664Register("r12d");
    public static final IRegister R13 = new X8664Register("r13d");
    public static final IRegister R14 = new X8664Register("r14d");
    public static final IRegister R15 = new X8664Register("r15d");

    
    private static final IRegister[] GENERAL_PURPOSE_REGISTERS = new IRegister[] { R8, R9, R10, R11, R12, R13, R14, R15 };

    @Override
    public String toString() {
        return "%" + this.id;
    }

    public static IRegister[] getGeneralPurposeRegisters() {
        return GENERAL_PURPOSE_REGISTERS.clone();
    }
}
