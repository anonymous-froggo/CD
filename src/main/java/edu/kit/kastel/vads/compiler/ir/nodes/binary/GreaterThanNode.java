package edu.kit.kastel.vads.compiler.ir.nodes.binary;

import edu.kit.kastel.vads.compiler.ir.nodes.Block;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;

public final class GreaterThanNode extends BinaryOperationNode {
public GreaterThanNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
