package edu.kit.kastel.vads.compiler.ir.nodes.control;

import edu.kit.kastel.vads.compiler.ir.nodes.Block;

public final class StartNode extends ControlFlowNode {

    public StartNode(Block block) {
        super(block);
    }

    @Override
    public Block target(int idx) {
        // A start node does not point to a block
        return null;
    }

    @Override
    public void setTarget(int idx, Block target) {
        // A start node does not point to a block
        return;
    }
}
