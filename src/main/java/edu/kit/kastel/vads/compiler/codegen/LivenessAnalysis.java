package edu.kit.kastel.vads.compiler.codegen;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.kit.kastel.vads.compiler.Main;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.util.GraphVizPrinter;

public class LivenessAnalysis {
    private static Map<Node, Set<Node>> def = new HashMap<>();
    private static Map<Node, Set<Node>> use = new HashMap<>();
    private static Map<Node, Set<Node>> succ = new HashMap<>();

    private static Map<Node, Set<Node>> live = new HashMap<>();
    private static boolean liveChanged;

    private static Set<Node> visited = new HashSet<>();
    private static Node lastNodeNeedingSucc;

    public static Map<Node, Integer> calculateNodeColors(IrGraph irGraph) {
        InterferenceGraph interferenceGraph = calculateInterferenceGraph(irGraph);
        interferenceGraph.color();
        return interferenceGraph.getNodeColors();
    }

    private static InterferenceGraph calculateInterferenceGraph(IrGraph irGraph) {
        // TODO: generate IR-Graph for each graph in the program
        if (Main.GENERATE_IR_GRAPH) {
            GraphVizPrinter.generateSvg(irGraph);
        }

        visited.add(irGraph.endBlock());
        scanForJRules(irGraph.endBlock());

        // System.out.println(def);
        // System.out.println(use);
        // System.out.println(succ);

        for (Node l : use.keySet()) {
            K1(l);
        }
        do {
            liveChanged = false;
            for (Node l : succ.keySet()) {
                K2(l);
            }
        } while (liveChanged);

        System.out.println("live: " + live);

        InterferenceGraph interferenceGraph = new InterferenceGraph(live);

        return interferenceGraph;
    }

    private static void scanForJRules(Node node) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scanForJRules(predecessor);
            }
        }

        switch (node) {
            case BinaryOperationNode binaryOperationNode -> J1(binaryOperationNode);
            case ReturnNode returnNode -> J2(returnNode);
            case ConstIntNode constIntNode -> J3(constIntNode);
            default -> {
                return;
            }
        }

        if (lastNodeNeedingSucc != null) {
            addFact(succ, lastNodeNeedingSucc, node);
        }

        if (node instanceof BinaryOperationNode || node instanceof ConstIntNode) {
            lastNodeNeedingSucc = node;
        }
    }

    private static void J1(BinaryOperationNode binaryOperationNode) {
        Node l = binaryOperationNode;

        Node x = binaryOperationNode;
        Node y = predecessorSkipProj(binaryOperationNode, BinaryOperationNode.LEFT);
        Node z = predecessorSkipProj(binaryOperationNode, BinaryOperationNode.RIGHT);

        addFact(def, l, x);
        addFact(use, l, y);
        addFact(use, l, z);
    }

    private static void J2(ReturnNode returnNode) {
        Node l = returnNode;

        Node x = predecessorSkipProj(returnNode, ReturnNode.RESULT);

        addFact(use, l, x);
    }

    private static void J3(ConstIntNode constIntNode) {
        Node l = constIntNode;

        Node x = constIntNode;

        addFact(def, l, x);
    }

    // Not yet necessary
    private static void J4() {

    }

    // Not yet necessary
    private static void J5() {

    }

    private static void K1(Node l) {
        for (Node x : use.get(l)) {
            addFact(live, l, x);
        }
    }

    private static void K2(Node l) {
        for (Node lPrime : succ.get(l)) {
            if (live.get(lPrime) == null) {
                continue;
            }

            for (Node u : live.get(lPrime)) {
                if (def.get(l) == null || !def.get(l).contains(u)) {
                    liveChanged = addFact(live, l, u) || liveChanged;
                }
            }
        }
    }

    private static boolean addFact(Map<Node, Set<Node>> predicate, Node line, Node subject) {
        if (!predicate.containsKey(line)) {
            predicate.put(line, new HashSet<>());
        }

        return predicate.get(line).add(subject);
    }
}
