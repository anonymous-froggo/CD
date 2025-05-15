package edu.kit.kastel.vads.compiler.codegen.x86_64;

import static edu.kit.kastel.vads.compiler.codegen.IRegisterAllocator.needsRegister;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.kit.kastel.vads.compiler.codegen.IRegister;
import edu.kit.kastel.vads.compiler.codegen.IRegisterAllocator;
import edu.kit.kastel.vads.compiler.codegen.LivenessAnalysis;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.util.GraphVizPrinter;

public class X8664RegisterAllocator implements IRegisterAllocator {
    private int id = 0;

    private IRegister[] registers;
    private final IrGraph irGraph;
    private final Map<Node, IRegister> registerAllocation = new HashMap<>();

    public X8664RegisterAllocator(IrGraph irGraph) {
        this.registers = X8664Register.getGeneralPurposeRegisters();
        this.irGraph = irGraph;
    }

    @Override
    public Map<Node, IRegister> allocateRegisters() {
        LivenessAnalysis.analyze(irGraph);
        Set<Node> visited = new HashSet<>();
        visited.add(irGraph.endBlock());
        scan(irGraph.endBlock(), visited);
        return Map.copyOf(this.registerAllocation);
    }

    private void scan(Node node, Set<Node> visited) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scan(predecessor, visited);
            }
        }
        if (needsRegister(node)) {
            this.registerAllocation.put(node, this.registers[(this.id++) % this.registers.length]);
        }
    }
}
