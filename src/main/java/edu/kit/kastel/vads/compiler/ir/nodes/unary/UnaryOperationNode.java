package edu.kit.kastel.vads.compiler.ir.nodes.unary;

import edu.kit.kastel.vads.compiler.ir.nodes.Block;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;

public sealed abstract class UnaryOperationNode extends Node permits
    LogicalNotNode,
    BitwiseNotNode,
    NegateNode
{
    protected UnaryOperationNode(Block block, Node input) {
        super(block, input);
    }
}
