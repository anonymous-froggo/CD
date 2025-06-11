package edu.kit.kastel.vads.compiler.ir.nodes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.nodes.control.ControlFlowNode;

public final class Block extends Node {

    private static int idCounter = 0;
    private final int id;

    private boolean isEmpty;

    private List<Node> nodes = new ArrayList<>();
    // The control flow exit point of this Block. Is null for graph().endBlock().
    private @Nullable ControlFlowNode controlFlowExit = null;
    // Contains the phis this block needs to write to in topological order,
    // mapped to the control flow index this block leads into
    private Map<Phi, Integer> phiIndices = new LinkedHashMap<>();

    public Block(IrGraph graph) {
        super(graph);

        this.id = idCounter++;

        this.isEmpty = true;
    }

    public List<Node> nodes() {
        return new ArrayList<>(this.nodes);
    }

    public void addNode(Node node) {
        if (node instanceof ControlFlowNode controlFlowNode) {
            // Each block can only have 1 control flow exit
            assert this.controlFlowExit == null
                : label() + " already has a control flow exit. '" + controlFlowNode + "' shouldn't be here.";

            // Don't add node to this.nodes, instead set this.controlFlowNode
            this.controlFlowExit = controlFlowNode;
        } else {
            this.nodes.add(node);
        }
    }

    public int phiIndex(Phi phi) {
        assert this.phiIndices.containsKey(phi) : phi + " not present in block " + this.label();

        return this.phiIndices.get(phi);
    }

    // Adds a phi from a successor block that needs to be written into from this
    // block
    public void addPhi(Phi phi, int index) {
        this.phiIndices.put(phi, index);
    }

    public void appendPhisAndControlFlowExit() {
        int size = this.phiIndices.keySet().size();

        List<Phi> phis = new ArrayList<>(size);
        // Collect phis
        for (Phi phi : this.phiIndices.keySet()) {
            phis.add(phi);
        }

        // Need to append phis in reverse topological order
        for (int i = size - 1; i >= 0; i--) {
            this.nodes.add(phis.get(i));
        }

        if (controlFlowExit != null) {
            this.nodes.add(controlFlowExit);
        }
    }

    @Nullable
    public ControlFlowNode controlFlowExit() {
        return this.controlFlowExit;
    }

    public String label() {
        return "block_" + this.id;
    }

    public int id() {
        return this.id;
    }

    public boolean isEmpty() {
        return this.isEmpty;
    }

    public void setNotEmpty() {
        this.isEmpty = false;
    }
}
