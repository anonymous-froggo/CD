package edu.kit.kastel.vads.compiler.codegen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.kit.kastel.vads.compiler.Main;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.binaryoperation.ModNode;

public class InterferenceGraph {
    // TODO implementing the interference graph using neighborhoods might not
    // be the most efficient approach, maybe try other stuff
    public static final int UNCOLORED = -1;

    private Map<Node, Set<Node>> neighborhoods;
    private Map<Node, Integer> nodeColors;

    private int numberOfColors = 0;

    public InterferenceGraph(Map<Node, Set<Node>> live) {
        this.neighborhoods = new HashMap<>();

        for (Set<Node> clique : live.values()) {
            numberOfColors = Math.max(clique.size(), numberOfColors);
            this.addClique(clique);
        }

        // Remove each node from its neighborhood
        for (Node node : this.neighborhoods.keySet()) {
            this.neighborhoods.get(node).remove(node);
        }
    }

    public void color() {
        // Input: G = (V, E) and ordered sequence v1, . . . , vn of nodes.
        List<Node> simplicialEliminationOrdering = this.maximumCardinalitySearch();
        // if (Main.DEBUG) System.out.println("simplicialEliminationOrdering: " +
        // simplicialEliminationOrdering);
        // if (Main.DEBUG) System.out.println("numberOfColors: " + this.numberOfColors);
        // if (Main.DEBUG) System.out.println("neighborhoods: " + this.neighborhoods);

        // Output: Assignment col : V → {0, ..., ∆(G)}.
        this.nodeColors = new HashMap<>(simplicialEliminationOrdering.size());
        for (Node node : simplicialEliminationOrdering) {
            this.nodeColors.put(node, UNCOLORED);
        }
        boolean[] colorAvailable = new boolean[this.numberOfColors];

        // For i ← 1 to n do
        for (Node vi : simplicialEliminationOrdering) {

            // Let c be the lowest color not used in N(vi)
            Arrays.fill(colorAvailable, true);
            for (Node u : this.neighborhoods.get(vi)) {
                int color = nodeColors.get(u);
                if (color != UNCOLORED) {
                    colorAvailable[color] = false;
                }
            }
            int c = UNCOLORED;
            for (int color = 0; color < this.numberOfColors; color++) {
                if (colorAvailable[color]) {
                    c = color;
                    break;
                }
            }

            // Set col(vi) ← c
            this.nodeColors.put(vi, c);
        }

        if (Main.DEBUG)
            System.out.println("numberOfColors: " + this.numberOfColors);
        if (Main.DEBUG)
            System.out.println("nodeColors: " + this.nodeColors);

        // for (Node node : this.neighborhoods.keySet()) {
        // if (Main.DEBUG) System.out.println(node + "=" + this.colors.get(node) + ":
        // ");
        // for (Node neighbor : this.neighborhoods.get(node)) {
        // if (Main.DEBUG) System.out.println("\t" + neighbor + "=" +
        // this.colors.get(neighbor));
        // }
        // }

        // if (Main.DEBUG) System.out.println("---------");
        // for (Node l : this.live.keySet()) {
        // if (Main.DEBUG) System.out.println("Clique of " + l);
        // for (Node node : this.live.get(l)) {
        // if (Main.DEBUG) System.out.println("\t" + node + "=" +
        // this.colors.get(node));
        // }
        // }
    }

    private List<Node> maximumCardinalitySearch() {
        // Input: G = (V, E) with |V| = n
        List<Node> V = new ArrayList<>(this.neighborhoods.keySet());
        int n = V.size();

        // Output: A simplicial elimination ordering v1, ..., vn
        List<Node> simplicialEliminationOrdering = new ArrayList<>(n);

        // For all v ∈ V set wt(v) ← 0
        Map<Node, Integer> wt = new HashMap<>(n);
        for (Node v : V) {
            wt.put(v, 0);
        }

        // Let W ← V
        List<Node> W = new ArrayList<>(V);

        // For i ← 1 to n do
        for (int i = 1; i <= n; i++) {
            // Let v be a node of maximal weight in W
            int max = -1;
            Node v = null;
            for (Node node : W) {
                if (wt.get(node) > max) {
                    max = wt.get(node);
                    v = node;
                }
            }

            // Set vi ← v
            simplicialEliminationOrdering.add(v);

            // For all u ∈ W ∩ N(v) set wt(u) ← wt(u) + 1
            for (Node u : W) {
                if (neighborhoods.get(v).contains(u)) {
                    wt.put(u, wt.get(u) + 1);
                }
            }

            // Set W ← W \ {v}
            W.remove(v);
        }

        return simplicialEliminationOrdering;
    }

    private void addClique(Set<Node> clique) {
        for (Node node : clique) {
            if (this.neighborhoods.get(node) == null) {
                this.neighborhoods.put(node, new HashSet<>());
            }

            this.neighborhoods.get(node).addAll(clique);
        }
    }

    public int getNumberOfColors() {
        return this.numberOfColors;
    }

    public Map<Node, Integer> getNodeColors() {
        return Map.copyOf(this.nodeColors);
    }
}
