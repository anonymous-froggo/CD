package edu.kit.kastel.vads.compiler.ir.nodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.nodes.control.ControlFlowNode;

public final class Block extends Node {

    private static int idCounter = 0;
    private final int id;

    private boolean isEmpty;

    private List<Node> schedule = new ArrayList<>();
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

    public List<Node> schedule() {
        return new ArrayList<>(this.schedule);
    }

    public void addToSchedule(Node node) {
        this.schedule.add(node);
    }

    public int phiIndex(Phi phi) {
        if (!this.phiIndices.containsKey(phi)) {
            throw new IllegalArgumentException(phi + " not present in block " + this.label());
        }

        return this.phiIndices.get(phi);
    }

    // Adds a phi from a successor block that needs to be written into from this
    // block
    public void addPhi(Phi phi, int index) {
        this.phiIndices.put(phi, index);
    }

    public void appendPhis() {
        int size = this.phiIndices.keySet().size();

        List<Phi> phis = new ArrayList<>(size);
        for (Phi phi : this.phiIndices.keySet()) {
            phis.add(phi);
        }

        // Need to append phis in reverse topological order
        for (int i = size - 1; i >= 0; i--) {
            this.schedule.add(phis.get(i));
        }
    }

    public void appendControlFlowExit() {
        if (controlFlowExit != null) {
            this.schedule.add(controlFlowExit);
        }
    }

    @Nullable
    public ControlFlowNode controlFlowExit() {
        return this.controlFlowExit;
    }

    public void setControlFlowExit(ControlFlowNode controlFlowExit) {
        if (this.controlFlowExit != null) {
            throw new IllegalArgumentException(
                label() + " already has a control flow exit. '" + controlFlowExit + "' shouldn't be here."
            );
        }

        this.controlFlowExit = controlFlowExit;
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
