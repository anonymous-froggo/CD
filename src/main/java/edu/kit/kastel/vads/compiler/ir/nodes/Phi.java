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

        List<Node> operands = operands();
        for (int i = 0; i < operands.size(); i++) {
            info.append(operands.get(i).toString());

            if (i < operands.size() - 1) {
                info.append(", ");
            }
        }
        info.append("]");

        return info.toString();
    }

    public boolean isSideEffectPhi() {
        // We only need to check the first predecessor since either none or all
        // predecessors are side effect projection nodes
        return switch (predecessor(0)) {
            // Check if projNode is of side effect type
            case ProjNode projNode -> projNode.projectionInfo() == SimpleProjectionInfo.SIDE_EFFECT;
            // Recursively check for side effects
            case Phi phi -> phi.isSideEffectPhi();
            // No side effect phi
            default -> false;
        };
    }

    public List<Node> operands() {
        return predecessorsSkipProj(this);
    }

    public void appendOperand(Node node) {
        addPredecessor(node);
    }
}
