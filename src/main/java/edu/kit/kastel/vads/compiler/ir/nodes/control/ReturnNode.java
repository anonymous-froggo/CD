package edu.kit.kastel.vads.compiler.ir.nodes.control;

import edu.kit.kastel.vads.compiler.ir.nodes.Block;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;

public final class ReturnNode extends ControlFlowNode {

    public static final int SIDE_EFFECT = 0;
    public static final int RESULT = 1;

    public ReturnNode(Block block, Node sideEffect, Node result) {
        super(block, sideEffect, result);
    }
}
