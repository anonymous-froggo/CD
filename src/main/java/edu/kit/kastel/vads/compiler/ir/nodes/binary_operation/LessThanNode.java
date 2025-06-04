package edu.kit.kastel.vads.compiler.ir.nodes.binary_operation;

import edu.kit.kastel.vads.compiler.ir.nodes.Block;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;

public final class LessThanNode extends BinaryOperationNode {
public LessThanNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
