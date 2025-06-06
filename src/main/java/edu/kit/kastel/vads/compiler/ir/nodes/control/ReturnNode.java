package edu.kit.kastel.vads.compiler.ir.nodes.control;

import java.util.Set;

import edu.kit.kastel.vads.compiler.ir.nodes.Block;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;

public final class ReturnNode extends ControlFlowNode {

    // Predecessor idx's
    public static final int SIDE_EFFECT = 0;
    public static final int RESULT = 1;

    // Successor idx's
    public static final int TARGET = 0;

    private Block target;

    public ReturnNode(Block block, Node sideEffect, Node result) {
        super(block, sideEffect, result);
    }

    @Override
    public Block target(int idx) {
        if (idx != TARGET) {
            throw new IllegalArgumentException("Not a valid target idx: " + idx);
        }

        return this.target;
    }

    @Override
    public void setTarget(int idx, Block target) {
        if (idx != TARGET) {
            throw new IllegalArgumentException("Not a valid target idx: " + idx);
        }

        this.target = target;
    }
}
