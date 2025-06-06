package edu.kit.kastel.vads.compiler.ir.nodes.control;

import edu.kit.kastel.vads.compiler.ir.nodes.Block;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;

public final class ConditionalJumpNode extends ControlFlowNode {

    public ConditionalJumpNode(Block block, Node condition) {
        super(block, condition);
    }
}
