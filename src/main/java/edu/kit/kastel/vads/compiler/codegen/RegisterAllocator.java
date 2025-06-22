package edu.kit.kastel.vads.compiler.codegen;

import edu.kit.kastel.vads.compiler.ir.nodes.Node;

public interface RegisterAllocator {

    void allocate();

    Register register(Node node);

    Register paramRegister(int id);
}
