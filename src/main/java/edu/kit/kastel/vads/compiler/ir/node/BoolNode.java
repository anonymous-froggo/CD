package edu.kit.kastel.vads.compiler.ir.node;

public final class BoolNode extends Node {

    private final boolean value;

    public BoolNode(Block block, boolean value) {
        super(block);
        this.value = value;
    }

    public boolean value() {
        return this.value();
    }

    // This is used in order to enable optimization -> if one constant is used
    // several times, it may as well just be the same node
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BoolNode other) {
            return this.block() == other.block() && this.value == other.value;
        }
        return false;
    }

    // This is used in order to enable optimization for hashmaps etc. -> if one
    // constant is used several times, it may as well just be the same node
    @Override
    public int hashCode() {
        return this.value ? 1 : 0;
    }

    @Override
    protected String info() {
        return "[" + this.value + "]";
    }
}
