package edu.kit.kastel.vads.compiler.ir.nodes;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorsSkipProj;

import java.util.List;

import edu.kit.kastel.vads.compiler.ir.nodes.ProjNode.SimpleProjectionInfo;

public final class Phi extends Node {

    public Phi(Block block) {
        super(block);
    }

    @Override
    public String info() {
        StringBuilder info = new StringBuilder("[");
        for (Node operand : this.operands()) {
            info.append(operand.toString())
                .append(", ");
        }
        info.append("]");

        return info.toString();
    }

    public boolean isSideEffectPhi() {
        // We only need to check the first predecessor since either none or all predecessors
        // are side effect projection nodes
        return predecessor(0) instanceof ProjNode projNode && projNode.projectionInfo() == SimpleProjectionInfo.SIDE_EFFECT;
    }

    public List<Node> operands() {
        return predecessorsSkipProj(this);
    }

    public void appendOperand(Node node) {
        addPredecessor(node);
    }
}
