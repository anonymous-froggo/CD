package edu.kit.kastel.vads.compiler.ir.nodes;

public final class DecisionNode extends Node {

    public static final int TRUE_SUCCESSOR = 0;
    public static final int FALSE_SUCCESSOR = 1;

    public DecisionNode(Block block, Node condition) {
        super(block, condition);
    }
}
