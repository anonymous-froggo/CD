package edu.kit.kastel.vads.compiler.ir.nodes.unary_operation;

import edu.kit.kastel.vads.compiler.ir.nodes.Block;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;

public final class NegateNode extends UnaryOperationNode {

    public NegateNode(Block block, Node input) {
        super(block, input);
    }
}
