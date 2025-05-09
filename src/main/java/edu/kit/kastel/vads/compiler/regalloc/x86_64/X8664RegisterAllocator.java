package edu.kit.kastel.vads.compiler.regalloc.x86_64;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.util.GraphVizPrinter;
import edu.kit.kastel.vads.compiler.regalloc.IRegister;
import edu.kit.kastel.vads.compiler.regalloc.IRegisterAllocator;
import edu.kit.kastel.vads.compiler.regalloc.aasm.AasmRegister;

import static edu.kit.kastel.vads.compiler.regalloc.IRegisterAllocator.needsRegister;

public class X8664RegisterAllocator implements IRegisterAllocator {
    private int id = 0;

    private IRegister[] registers;
    private final IrGraph inputIrGraph;
    private final Map<Node, IRegister> registerAllocation = new HashMap<>();

    public X8664RegisterAllocator(IrGraph inputIrGraph) {
        this.registers = X8664Register.getGeneralPurposeRegisters();
        this.inputIrGraph = inputIrGraph;
    }

    //TODO: this is just a dummy implementation, should implement liveness analysis + graph coloring for regalloc
    @Override
    public Map<Node, IRegister> allocateRegisters() {
        Set<Node> visited = new HashSet<>();
        visited.add(inputIrGraph.endBlock());
        scan(inputIrGraph.endBlock(), visited);
        return Map.copyOf(this.registerAllocation);
    }

    private void scan(Node node, Set<Node> visited) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scan(predecessor, visited);
            }
        }
        if (needsRegister(node)) {
            this.registerAllocation.put(node, this.registers[this.id++]);
        }
    }
}
