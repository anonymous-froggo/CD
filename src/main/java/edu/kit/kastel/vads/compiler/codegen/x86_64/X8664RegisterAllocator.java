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
import edu.kit.kastel.vads.compiler.ir.SsaGraph;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;

public class X8664RegisterAllocator implements RegisterAllocator {

    private final SsaGraph graph;

    private final List<Register> registers;
    private final Map<Node, Register> registerAllocation = new HashMap<>();

    private int nStackRegisters;

    public X8664RegisterAllocator(SsaGraph graph) {
        this.registers = new ArrayList<>(Arrays.asList(X8664Register.generalPurposeRegisters()));
        this.graph = graph;
    }

    @Override
    public void allocate() {
        InterferenceGraph interferenceGraph = new LivenessAnalysis(graph).calculateInterferenceGraph();
        interferenceGraph.color();

        Map<Node, Integer> nodeColors = interferenceGraph.getNodeColors();
        int numberOfColors = interferenceGraph.getNumberOfColors();
        this.nStackRegisters = Math.max(0, numberOfColors - this.registers.size());
        for (int i = 0; i < this.nStackRegisters; i++) {
            Register stackRegister = new X8664StackRegister(i);
            this.registers.add(stackRegister);
        }
        for (Node node : nodeColors.keySet()) {
            this.registerAllocation.put(node, registers.get(nodeColors.get(node)));
        }
    }

    @Override
    public Register register(Node node) {
        return this.registerAllocation.get(node);
    }

    @Override
    public Register paramRegister(int id) {
        return new X8664StackRegister(1 + id + numberOfStackRegisters() + X8664Register.calleeSavedRegisters().length);
    }

    public int numberOfStackRegisters() {
        return this.nStackRegisters;
    }

    public void printAllocation() {
        System.out.println("\nRegister allocation:");
        for (Node node : this.registerAllocation.keySet()) {
            System.out.println(node + " = " + this.registerAllocation.get(node).name(64));
        }
        System.out.println();
    }
}
