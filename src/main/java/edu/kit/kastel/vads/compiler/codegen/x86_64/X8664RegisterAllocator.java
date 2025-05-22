package edu.kit.kastel.vads.compiler.codegen.x86_64;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.kit.kastel.vads.compiler.codegen.IRegister;
import edu.kit.kastel.vads.compiler.codegen.IRegisterAllocator;
import edu.kit.kastel.vads.compiler.codegen.InterferenceGraph;
import edu.kit.kastel.vads.compiler.codegen.LivenessAnalysis;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.Node;

public class X8664RegisterAllocator implements IRegisterAllocator {
    private List<IRegister> registers;
    private final IrGraph irGraph;
    private final Map<Node, IRegister> registerAllocation = new HashMap<>();
    private int numberOfStackRegisters;

    public X8664RegisterAllocator(IrGraph irGraph) {
        this.registers = new ArrayList<>(Arrays.asList(X8664Register.getGeneralPurposeRegisters()));
        this.irGraph = irGraph;
    }

    @Override
    public Map<Node, IRegister> allocateRegisters() {
        InterferenceGraph interferenceGraph = LivenessAnalysis.calculateInterferenceGraph(irGraph);
        interferenceGraph.color();

        Map<Node, Integer> nodeColors = interferenceGraph.getNodeColors();
        int numberOfColors = interferenceGraph.getNumberOfColors();

        this.numberOfStackRegisters = Math.max(0, numberOfColors - this.registers.size());

        for (int i = 0; i < this.numberOfStackRegisters; i++) {
            IRegister stackRegister = new X8664StackRegister(i * 8);
            this.registers.add(stackRegister);
        }

        for (Node node : nodeColors.keySet()) {
            registerAllocation.put(node, registers.get(nodeColors.get(node)));
        }

        return Map.copyOf(this.registerAllocation);
    }

    public int getNumberOfStackRegisters() {
        return this.numberOfStackRegisters;
    }
}
