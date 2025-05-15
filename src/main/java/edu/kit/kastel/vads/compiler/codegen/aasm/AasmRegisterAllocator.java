package edu.kit.kastel.vads.compiler.codegen.aasm;

import edu.kit.kastel.vads.compiler.codegen.IRegister;
import edu.kit.kastel.vads.compiler.codegen.IRegisterAllocator;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.Node;

import static edu.kit.kastel.vads.compiler.codegen.IRegisterAllocator.needsRegister;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AasmRegisterAllocator implements IRegisterAllocator {
    private int id;
    private final Map<Node, IRegister> registers = new HashMap<>();
    private final IrGraph inputIrGraph;

    public AasmRegisterAllocator(IrGraph inputIrGraph) {
        this.inputIrGraph = inputIrGraph;
    }

    @Override
    public Map<Node, IRegister> allocateRegisters() {
        Set<Node> visited = new HashSet<>();
        visited.add(inputIrGraph.endBlock());
        scan(inputIrGraph.endBlock(), visited);
        return Map.copyOf(this.registers);
    }

    private void scan(Node node, Set<Node> visited) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scan(predecessor, visited);
            }
        }
        if (needsRegister(node)) {
            this.registers.put(node, new AasmRegister(this.id++));
        }
    }
}
