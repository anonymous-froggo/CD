package edu.kit.kastel.vads.compiler.codegen.x86_64;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.kit.kastel.vads.compiler.codegen.Register;
import edu.kit.kastel.vads.compiler.codegen.RegisterAllocator;
import edu.kit.kastel.vads.compiler.codegen.InterferenceGraph;
import edu.kit.kastel.vads.compiler.codegen.LivenessAnalysis;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;

public class X8664RegisterAllocator implements RegisterAllocator {

    private List<Register> registers;
    private final IrGraph graph;
    private final Map<Node, Register> registerAllocation = new HashMap<>();
    private int numberOfStackRegisters;

    public X8664RegisterAllocator(IrGraph graph) {
        this.registers = new ArrayList<>(Arrays.asList(X8664Register.getGeneralPurposeRegisters()));
        this.graph = graph;
    }

    @Override
    public Map<Node, Register> allocateRegisters() {
        InterferenceGraph interferenceGraph = new LivenessAnalysis(graph).calculateInterferenceGraph();
        interferenceGraph.color();

        Map<Node, Integer> nodeColors = interferenceGraph.getNodeColors();
        int numberOfColors = interferenceGraph.getNumberOfColors();

        this.numberOfStackRegisters = Math.max(0, numberOfColors - this.registers.size());

        for (int i = 0; i < this.numberOfStackRegisters; i++) {
            Register stackRegister = new X8664StackRegister(i * 8);
            this.registers.add(stackRegister);
        }

        for (Node node : nodeColors.keySet()) {
            registerAllocation.put(node, registers.get(nodeColors.get(node)));
        }

        return Map.copyOf(this.registerAllocation);
    }

    public int numberOfStackRegisters() {
        return this.numberOfStackRegisters;
    }
}
