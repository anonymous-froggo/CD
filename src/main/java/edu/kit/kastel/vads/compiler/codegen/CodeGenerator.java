package edu.kit.kastel.vads.compiler.codegen;

import java.util.List;

import edu.kit.kastel.vads.compiler.codegen.x86_64.X8664CodeGenerator;
import edu.kit.kastel.vads.compiler.ir.IrGraph;

public sealed interface CodeGenerator permits X8664CodeGenerator {

    String generateCode(List<IrGraph> program);

    String fromInt(int value);

    String fromBoolean(boolean value);
}
