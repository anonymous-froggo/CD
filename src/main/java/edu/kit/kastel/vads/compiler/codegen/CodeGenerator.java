package edu.kit.kastel.vads.compiler.codegen;

import edu.kit.kastel.vads.compiler.codegen.x86_64.X8664CodeGenerator;

public sealed interface CodeGenerator permits X8664CodeGenerator {

    String generateCode();

    String fromInt(int value);

    String fromBoolean(boolean value);
}
