package edu.kit.kastel.vads.compiler.ir.nodes.binary_operation;

import edu.kit.kastel.vads.compiler.ir.nodes.Block;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;

public final class BitwiseAndNode extends BinaryOperationNode {
    public BitwiseAndNode(Block block, Node left, Node right) {
        super(block, left, right);
    }

    @SuppressWarnings("EqualsDoesntCheckParameterClass") // we do, but not here
    @Override
    public boolean equals(Object obj) {
        return commutativeEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return commutativeHashCode(this);
    }
}
