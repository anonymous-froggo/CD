package edu.kit.kastel.vads.compiler.ir.nodes.control;

import edu.kit.kastel.vads.compiler.ir.nodes.Block;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;

public final class ConditionalJumpNode extends ControlFlowNode {

    // Predecessor idx's
    public static final int CONDITION = 0;

    // Successor idx's
    public static final int TRUE_TARGET = 0;
    public static final int FALSE_TARGET = 1;

    private Block trueTarget;
    private Block falseTarget;

    public ConditionalJumpNode(Block block, Node condition) {
        super(block, condition);
    }

    @Override
    public Block target(int idx) {
        return switch (idx) {
            case TRUE_TARGET -> this.trueTarget;
            case FALSE_TARGET -> this.falseTarget;
            default -> throw new IllegalArgumentException("Not a valid target idx: " + idx);
        };
    }

    @Override
    public void setTarget(int idx, Block target) {
        switch (idx) {
            case TRUE_TARGET -> this.trueTarget = target;
            case FALSE_TARGET -> this.falseTarget = target;
            default -> throw new IllegalArgumentException("Not a valid target idx: " + idx);
        }
    }
}
