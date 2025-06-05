package edu.kit.kastel.vads.compiler.ir.nodes.control;

import edu.kit.kastel.vads.compiler.ir.nodes.Block;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;

public final class ConditionalJumpNode extends Node {

    public ConditionalJumpNode(Block block, Node condition) {
        super(block, condition);
    }
}
