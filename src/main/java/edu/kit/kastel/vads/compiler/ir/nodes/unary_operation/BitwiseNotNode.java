package edu.kit.kastel.vads.compiler.ir.nodes.unary_operation;

import edu.kit.kastel.vads.compiler.ir.nodes.Block;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;

public final class BitwiseNotNode extends UnaryOperationNode {

    public BitwiseNotNode(Block block, Node input) {
        super(block, input);
    }
}
