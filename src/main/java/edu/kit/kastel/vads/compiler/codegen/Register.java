package edu.kit.kastel.vads.compiler.codegen;

import edu.kit.kastel.vads.compiler.codegen.x86_64.X8664Register;
import edu.kit.kastel.vads.compiler.codegen.x86_64.X8664StackRegister;

public sealed interface Register permits X8664Register, X8664StackRegister {

    String name(int bitlength);
}
