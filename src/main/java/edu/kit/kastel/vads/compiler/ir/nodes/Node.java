package edu.kit.kastel.vads.compiler.ir.nodes;

import edu.kit.kastel.vads.compiler.ir.util.DebugInfo;
import edu.kit.kastel.vads.compiler.ir.SsaGraph;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.nodes.control.ControlFlowNode;
import edu.kit.kastel.vads.compiler.ir.nodes.unary.UnaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.util.DebugInfoHelper;

import java.util.ArrayList;
import java.util.List;

/// The base class for all nodes.
public sealed abstract class Node permits
        BinaryOperationNode,
        Block,
        BoolNode,
        CallNode,
        ControlFlowNode,
        ConstIntNode,
        ParamNode,
        Phi,
        ProjNode,
        UnaryOperationNode {

    private final SsaGraph graph;
    private final Block block;
    private final List<Node> predecessors = new ArrayList<>();
    private final DebugInfo debugInfo;

    protected Node(Block block, Node... predecessors) {
        this(block, List.of(predecessors));
    }

    protected Node(Block block, List<Node> predecessors) {
        this.graph = block.graph();
        this.block = block;
        this.predecessors.addAll(predecessors);
        for (Node predecessor : predecessors) {
            graph.registerSuccessor(predecessor, this);
        }
        this.debugInfo = DebugInfoHelper.debugInfo();

        block.setNotEmpty();
    }

    protected Node(SsaGraph graph) {
        assert this.getClass() == Block.class : "must be used by Block only";
        this.graph = graph;
        this.block = (Block) this;
        this.debugInfo = DebugInfo.NoInfo.INSTANCE;
    }

    public final SsaGraph graph() {
        return this.graph;
    }

    public final Block block() {
        return this.block;
    }

    public final List<? extends Node> predecessors() {
        return List.copyOf(this.predecessors);
    }

    public final void setPredecessor(int idx, Node node) {
        this.graph.removeSuccessor(this.predecessors.get(idx), this);
        this.predecessors.set(idx, node);
        this.graph.registerSuccessor(node, this);
    }

    public final void addPredecessor(Node node) {
        this.predecessors.add(node);
        this.graph.registerSuccessor(node, this);
    }

    public final Node predecessor(int idx) {
        return this.predecessors.get(idx);
    }

    @Override
    public final String toString() {
        return (this.getClass().getSimpleName().replace("Node", "") + " " + info()).stripTrailing();
    }

    protected String info() {
        return "";
    }

    public DebugInfo debugInfo() {
        return debugInfo;
    }

    protected static int predecessorHash(Node node, int predecessor) {
        return System.identityHashCode(node.predecessor(predecessor));
    }
}
