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

        List<Node> operands = operands();
        for (int i = 0; i < operands.size(); i++) {
            Node operand = operands.get(i);
            if (operand instanceof Phi) {
                info.append("Phi");
                continue;
            }

            info.append(operands.get(i).toString());

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
        this.isSideEffectPhi = true;
    }

    public List<Node> operands() {
        return predecessorsSkipProj(this);
    }

    public void appendOperand(Node node) {
        addPredecessor(node);
    }
}
