package edu.kit.kastel.vads.compiler.ir.node.binaryoperation;

import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.Node;

public final class ShiftRightNode extends BinaryOperationNode {

    public ShiftRightNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
