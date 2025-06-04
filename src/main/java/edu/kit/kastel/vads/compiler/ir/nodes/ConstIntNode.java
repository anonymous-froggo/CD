package edu.kit.kastel.vads.compiler.ir.nodes;

public final class ConstIntNode extends Node {

    private final int value;

    public ConstIntNode(Block block, int value) {
        super(block);
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    // This is used in order to enable optimization -> if one constant is used
    // several times, it may as well just be the same node
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ConstIntNode other) {
            return this.block() == other.block() && this.value == other.value;
        }
        return false;
    }

    // This is used in order to enable optimization for hashmaps etc. -> if one
    // constant is used several times, it may as well just be the same node
    @Override
    public int hashCode() {
        return this.value;
    }

    @Override
    protected String info() {
        return "[" + this.value + "]";
    }
}
