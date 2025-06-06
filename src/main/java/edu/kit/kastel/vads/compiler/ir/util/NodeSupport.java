package edu.kit.kastel.vads.compiler.ir.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.kit.kastel.vads.compiler.ir.nodes.Node;
import edu.kit.kastel.vads.compiler.ir.nodes.ProjNode;
import edu.kit.kastel.vads.compiler.ir.nodes.control.ConditionalJumpNode;
import edu.kit.kastel.vads.compiler.ir.nodes.control.JumpNode;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;

public final class NodeSupport {

    private NodeSupport() {
    }

    public static Node predecessorSkipProj(Node node, int predIdx) {
        Node predecessor = node.predecessor(predIdx);

        if (predecessor instanceof ProjNode) {
            return predecessor.predecessor(ProjNode.IN);
        }

        return predecessor;
    }

    public static List<Node> predecessorsSkipProj(Node node) {
        List<Node> predecessors = new ArrayList<>();

        for (Node predecessor : node.predecessors()) {
            if (predecessor instanceof ProjNode) {
                predecessors.add(predecessor.predecessor(ProjNode.IN));
            } else {
                predecessors.add(predecessor);
            }
        }

        return predecessors;
    }
}
