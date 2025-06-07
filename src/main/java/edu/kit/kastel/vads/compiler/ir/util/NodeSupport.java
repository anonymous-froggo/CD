package edu.kit.kastel.vads.compiler.ir.util;

import java.util.ArrayList;
import java.util.List;

import edu.kit.kastel.vads.compiler.ir.nodes.Node;
import edu.kit.kastel.vads.compiler.ir.nodes.ProjNode;

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

    public static List<Node> successorsSkipProj(Node node) {
        List<Node> successors = new ArrayList<>();

        for (Node child : node.graph().successors(node)) {
            if (child instanceof ProjNode) {
                for (Node grandchild : child.graph().successors(child)) {
                    successors.add(grandchild);
                }
            } else {
                successors.add(child);
            }
        }

        return successors;
    }
}
