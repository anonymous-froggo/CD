package edu.kit.kastel.vads.compiler.ir;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.Set;

import edu.kit.kastel.vads.compiler.ir.nodes.Block;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;
import edu.kit.kastel.vads.compiler.ir.nodes.ParamNode;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;

public class SsaGraph {

    private final Name name;

    private final Map<Node, SequencedSet<Node>> successors = new IdentityHashMap<>();

    private final List<Block> blocks = new ArrayList<>();
    private final Block startBlock;
    private final Block endBlock;

    private final List<ParamNode> params = new ArrayList<>();

    public SsaGraph(Name name) {
        this.name = name;
        
        this.startBlock = new Block(this);
        this.endBlock = new Block(this);
    }

    public void registerSuccessor(Node node, Node successor) {
        this.successors.computeIfAbsent(node, _ -> new LinkedHashSet<>()).add(successor);
    }

    public void removeSuccessor(Node node, Node oldSuccessor) {
        this.successors.computeIfAbsent(node, _ -> new LinkedHashSet<>()).remove(oldSuccessor);
    }

    /// {@return the set of nodes that have the given node as one of their inputs}
    public Set<Node> successors(Node node) {
        SequencedSet<Node> successors = this.successors.get(node);
        if (successors == null) {
            return Set.of();
        }
        return Set.copyOf(successors);
    }

    public List<Block> blocks() {
        return new ArrayList<>(this.blocks);
    }

    public void addBlock(Block block) {
        this.blocks.add(block);
    }

    public Block startBlock() {
        return this.startBlock;
    }

    public Block endBlock() {
        return this.endBlock;
    }

    public void addParam(ParamNode param) {
        this.params.add(param);
    }

    public List<ParamNode> params() {
        return List.copyOf(this.params);
    }

    /// {@return the name of this graph}
    public String name() {
        return this.name.asString();
    }
}
