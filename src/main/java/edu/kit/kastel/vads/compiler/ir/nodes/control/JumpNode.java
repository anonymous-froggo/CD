package edu.kit.kastel.vads.compiler.ir.nodes.control;

import edu.kit.kastel.vads.compiler.ir.nodes.Block;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;

public final class JumpNode extends Node {

    public JumpNode(Block block) {
        super(block);
    }
}
