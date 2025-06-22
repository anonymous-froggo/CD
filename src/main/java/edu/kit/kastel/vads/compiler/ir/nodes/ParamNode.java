package edu.kit.kastel.vads.compiler.ir.nodes;

import edu.kit.kastel.vads.compiler.ir.nodes.control.StartNode;

public final class ParamNode extends Node {
    private final int id;

    public ParamNode(Block block, StartNode startNode, int id) {
        super(block, startNode);

        this.id = id;
    }

    @Override
    protected String info() {
        return "#" + id();
    }

    public int id() {
        return this.id;
    }
}
