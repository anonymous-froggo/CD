package edu.kit.kastel.vads.compiler.ir.nodes.binary;

import edu.kit.kastel.vads.compiler.ir.nodes.Block;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;

public final class ShiftRightNode extends BinaryOperationNode {

    public ShiftRightNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
