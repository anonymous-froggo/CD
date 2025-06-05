package edu.kit.kastel.vads.compiler.ir.nodes.unary;

import edu.kit.kastel.vads.compiler.ir.nodes.Block;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;

public final class LogicalNotNode extends UnaryOperationNode {

    public LogicalNotNode(Block block, Node input) {
        super(block, input);
    }
}
