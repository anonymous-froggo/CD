package edu.kit.kastel.vads.compiler.ir.nodes.binary_operation;

import edu.kit.kastel.vads.compiler.ir.nodes.Block;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;

public final class LessThanEqNode extends BinaryOperationNode {
public LessThanEqNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
