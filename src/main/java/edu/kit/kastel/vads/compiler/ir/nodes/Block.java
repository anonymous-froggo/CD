package edu.kit.kastel.vads.compiler.ir.nodes;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.nodes.control.ControlFlowNode;

public final class Block extends Node {

    private static int idCounter = 0;
    private final int id;

    private List<Node> nodes = new ArrayList<>();
    // The control flow exit point of this Block. Is null for graph().endBlock()
    private @Nullable ControlFlowNode controlFlowExit = null;

    public Block(IrGraph graph) {
        super(graph);
        this.id = idCounter++;
    }

    public List<Node> nodes() {
        return new ArrayList<>(this.nodes);
    }

    public void addNode(Node node) {
        if (node instanceof ControlFlowNode controlFlowNode) {
            // Don't add node to this.nodes, instead set this.controlFlowNode
            if (this.controlFlowExit != null) {
                // This normally shouldn't happen
                throw new IllegalArgumentException(
                    label() + " already has a control flow exit. '" + controlFlowNode + "' shouldn't be here."
                );
            }

            this.controlFlowExit = controlFlowNode;
        } else {
            this.nodes.add(node);
        }
    }

    public void appendControlFlowExit() {
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
}
