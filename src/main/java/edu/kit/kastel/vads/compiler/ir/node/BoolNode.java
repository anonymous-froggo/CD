package edu.kit.kastel.vads.compiler.ir.node;

public final class BoolNode extends Node {

    private final int value;

    public BoolNode(Block block, int value) {
        super(block);
        this.value = value;
    }

    public int value() {
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

    // TODO implement optimization similar to {ConstIntNode}
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    protected String info() {
        return "[" + this.value + "]";
    }
}
