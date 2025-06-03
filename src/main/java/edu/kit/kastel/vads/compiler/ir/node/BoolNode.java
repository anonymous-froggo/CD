package edu.kit.kastel.vads.compiler.ir.node;

public final class BoolNode extends Node {

    private final boolean value;

    protected BoolNode(Block block, boolean value) {
        super(block);
        this.value = value;
    }

    public boolean value() {
        return this.value();
    }
}
