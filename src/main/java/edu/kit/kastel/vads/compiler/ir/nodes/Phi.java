package edu.kit.kastel.vads.compiler.ir.nodes;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorsSkipProj;

import java.util.List;

public final class Phi extends Node {

    private boolean isSideEffectPhi = false;

    public Phi(Block block) {
        super(block);
    }

    @Override
    public String info() {
        StringBuilder info = new StringBuilder("[");

        List<Node> operands = predecessorsSkipProj(this);
        for (int i = 0; i < operands.size(); i++) {
            Node operand = operands.get(i);
            if (operand instanceof Phi) {
                info.append("Phi");
            } else {
                info.append(operands.get(i).toString());
            }
            
            if (i < operands.size() - 1) {
                info.append(", ");
            }
        }
        info.append("]");

        return info.toString();
    }

    public boolean isSideEffectPhi() {
        return isSideEffectPhi;
    }

    public void setSideEffectPhi() {
        if (isSideEffectPhi()) {
            return;
        }

        this.isSideEffectPhi = true;
        for (Node operand : predecessorsSkipProj(this)) {
            if (operand instanceof Phi phi) {
                phi.setSideEffectPhi();
            }
        }
    }

    public void appendOperand(Node node) {
        addPredecessor(node);

        if (node instanceof Phi phi && isSideEffectPhi()) {
            phi.setSideEffectPhi();
        }
    }
}
