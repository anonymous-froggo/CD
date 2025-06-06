package edu.kit.kastel.vads.compiler.ir.nodes.control;

import edu.kit.kastel.vads.compiler.ir.nodes.Block;

public final class JumpNode extends ControlFlowNode {

    // Successor idx's
    public static final int TARGET = 0;

    private Block target;

    public JumpNode(Block block) {
        super(block);
    }

    @Override
    public Block target(int idx) throws IllegalArgumentException {
        if (idx != TARGET) {
            throw new IllegalArgumentException("Not a valid target idx: " + idx);
        }

        return this.target;
    }

    @Override
    public void setTarget(int idx, Block target) throws IllegalArgumentException {
        if (idx != TARGET) {
            throw new IllegalArgumentException("Not a valid target idx: " + idx);
        }

        this.target = target;
    }
}
