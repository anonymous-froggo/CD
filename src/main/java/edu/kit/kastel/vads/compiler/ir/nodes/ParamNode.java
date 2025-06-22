package edu.kit.kastel.vads.compiler.ir.nodes;

public final class ParamNode extends Node {
    private final int id;

    public ParamNode(Block block, int id) {
        super(block);

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
