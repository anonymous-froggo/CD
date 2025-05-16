package edu.kit.kastel.vads.compiler.codegen;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.kit.kastel.vads.compiler.Main;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;
import edu.kit.kastel.vads.compiler.ir.util.GraphVizPrinter;

public class LivenessAnalysis {
    private static Map<Node, Set<Node>> def = new HashMap<>();
    private static Map<Node, Set<Node>> use = new HashMap<>();
    private static Map<Node, Set<Node>> succ = new HashMap<>();

    private static Map<Node, Set<Node>> live = new HashMap<>();
    private static boolean liveChanged;

    private static Set<Node> visited = new HashSet<>();
    private static List<Node> schedule = new ArrayList<>();

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
        scan(irGraph.endBlock());
        System.out.println(schedule);

        for (int l = schedule.size() - 1; l >= 0; l--) {
            Node node = schedule.get(l);
            System.out.println(node);
            switch (node) {
                case BinaryOperationNode binaryOperationNode -> J1(binaryOperationNode, schedule.get(l + 1));
                case ReturnNode returnNode -> J2(returnNode);
                case ConstIntNode constIntNode -> J3(constIntNode, schedule.get(l + 1));
                case ProjNode projNode -> J6(projNode, schedule.get(l + 1));
                default -> {
                }
            }
        }

        // System.out.println("def: " + def);
        System.out.println("use: " + use);
        System.out.println("succ: " + succ);

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

    private static void scan(Node node) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scan(predecessor);
            }
        }

        schedule.add(node);
    }

    private static void J1(BinaryOperationNode binaryOperationNode, Node lPlusOne) {
        Node l = binaryOperationNode;

        Node x = binaryOperationNode;
        Node y = predecessorSkipProj(binaryOperationNode, BinaryOperationNode.LEFT);
        Node z = predecessorSkipProj(binaryOperationNode, BinaryOperationNode.RIGHT);

        addFact(def, l, x);
        addFact(use, l, y);
        addFact(use, l, z);
        addFact(succ, l, lPlusOne);
    }

    private static void J2(ReturnNode returnNode) {
        Node l = returnNode;

        Node x = predecessorSkipProj(returnNode, ReturnNode.RESULT);

        addFact(use, l, x);
    }

    private static void J3(ConstIntNode constIntNode, Node lPlusOne) {
        Node l = constIntNode;

        Node x = constIntNode;

        addFact(def, l, x);
        addFact(succ, l, lPlusOne);
    }

    // Not yet necessary
    private static void J4() {

    }

    // Not yet necessary
    private static void J5() {

    }

    // Additional rule needed for projNodes
    private static void J6(ProjNode projNode, Node lPlusOne) {
        Node l = projNode;

        Node x = projNode.predecessor(ProjNode.IN);

        addFact(use, l, x);
        addFact(succ, l, lPlusOne);
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
