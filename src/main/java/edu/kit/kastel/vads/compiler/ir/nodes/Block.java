package edu.kit.kastel.vads.compiler.ir.nodes;

import edu.kit.kastel.vads.compiler.ir.IrGraph;

public final class Block extends Node {
    private final String info;

    public Block(int id, IrGraph graph) {
        super(graph);
        this.info = String.valueOf(id);
    }

    @Override
    protected String info() {
        return "[" + this.info + "]";
    }
}
