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
import edu.kit.kastel.vads.compiler.ir.node.binaryoperation.DivNode;
import edu.kit.kastel.vads.compiler.ir.node.binaryoperation.ModNode;
import edu.kit.kastel.vads.compiler.ir.util.GraphVizPrinter;

public class LivenessAnalysis {
    private static Map<Node, Set<Node>> def = new HashMap<>();
    private static Map<Node, Set<Node>> use = new HashMap<>();
    private static Map<Node, Set<Node>> succ = new HashMap<>();

    private static Map<Node, Set<Node>> live = new HashMap<>();
    private static boolean liveChanged;

    private static Set<Node> visited = new HashSet<>();
    private static List<Node> schedule = new ArrayList<>();

    public static InterferenceGraph calculateInterferenceGraph(IrGraph irGraph) {
        // TODO: generate IR-Graph for each graph in the program
        if (Main.DEBUG) {
            GraphVizPrinter.generateSvg(irGraph);
        }

        visited.add(irGraph.endBlock());
        scan(irGraph.endBlock());
        if (Main.DEBUG) {
            System.out.println("schedule: " + schedule);
        }

        for (int l = schedule.size() - 1; l >= 0; l--) {
            Node node = schedule.get(l);
            switch (node) {
                case BinaryOperationNode binaryOperationNode -> J1(binaryOperationNode, schedule.get(l + 1));
                case ReturnNode returnNode -> J2(returnNode);
                case ConstIntNode constIntNode -> J3(constIntNode, schedule.get(l + 1));
                default -> {
                }
            }
        }

        if (Main.DEBUG) {
            System.out.println("def: " + def);
            System.out.println("use: " + use);
            System.out.println("succ: " + succ);
        }

        for (Node l : use.keySet()) {
            K1(l);
        }
        do {
            liveChanged = false;
            for (Node l : succ.keySet()) {
                K2(l);
            }
        } while (liveChanged);

        if (Main.DEBUG) {
            System.out.println("live: " + live);
        }

        InterferenceGraph interferenceGraph = new InterferenceGraph(live);
        return interferenceGraph;
    }

    private static void scan(Node node) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scan(predecessor);
            }
        }

        if (!(node instanceof ProjNode || node instanceof StartNode || node instanceof Block)) {
            schedule.add(node);
        }
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

        if (binaryOperationNode instanceof DivNode) {
            Node sideEffect = predecessorSkipProj(binaryOperationNode, DivNode.SIDE_EFFECT);
            addSideEffectUse(l, sideEffect);
        } else if (binaryOperationNode instanceof ModNode) {
            Node sideEffect = predecessorSkipProj(binaryOperationNode, ModNode.SIDE_EFFECT);
            addSideEffectUse(l, sideEffect);
        }
    }

    private static void J2(ReturnNode returnNode) {
        Node l = returnNode;

        Node x = predecessorSkipProj(returnNode, ReturnNode.RESULT);

        addFact(use, l, x);

        // I don't know if this is needed
        Node sideEffect = predecessorSkipProj(returnNode, ReturnNode.SIDE_EFFECT);
        addSideEffectUse(l, sideEffect);
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

    private static void addSideEffectUse(Node l, Node sideEffect) {
        if (sideEffect instanceof StartNode) {
            return;
        }

        addFact(use, l, sideEffect);
    }
}
