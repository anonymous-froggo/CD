package edu.kit.kastel.vads.compiler.codegen.x86_64;

import static edu.kit.kastel.vads.compiler.codegen.IRegisterAllocator.needsRegister;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.kit.kastel.vads.compiler.codegen.IRegister;
import edu.kit.kastel.vads.compiler.codegen.IRegisterAllocator;
import edu.kit.kastel.vads.compiler.codegen.InterferenceGraph;
import edu.kit.kastel.vads.compiler.codegen.LivenessAnalysis;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.Node;

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
        Map<Node, Integer> nodeColors = LivenessAnalysis.calculateNodeColors(irGraph);

        // TODO implement spilling

        for (Node node : nodeColors.keySet()) {
            registerAllocation.put(node, registers[nodeColors.get(node)]);
        }

        return Map.copyOf(this.registerAllocation);
    }
}
