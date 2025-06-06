package edu.kit.kastel.vads.compiler.ir.nodes;

import java.util.ArrayList;
import java.util.List;

import edu.kit.kastel.vads.compiler.ir.IrGraph;

public final class Block extends Node {
    private static int idCounter = 0;
    private final int id;

    private List<Node> nodes;

    public Block(IrGraph graph) {
        super(graph);
        this.id = idCounter++;

        this.nodes = new ArrayList<>();
    }

    public void addNode(Node node) {
        this.nodes.add(node);
    }

    @Override
    public String info() {
        return "[" + this.id + "]";
    }

    public int id() {
        return this.id;
    }

    public List<Node> nodes() {
        return new ArrayList<>(this.nodes);
    }
}
