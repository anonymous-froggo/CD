package edu.kit.kastel.vads.compiler.ir.nodes.control;

import edu.kit.kastel.vads.compiler.ir.nodes.Block;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;

public sealed abstract class ControlFlowNode extends Node permits
    ConditionalJumpNode,
    JumpNode,
    ReturnNode, 
    StartNode
{
    public ControlFlowNode(Block block, Node... predecessors) {
        super(block, predecessors);
    }
}
