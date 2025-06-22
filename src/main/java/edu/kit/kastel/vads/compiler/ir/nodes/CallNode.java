package edu.kit.kastel.vads.compiler.ir.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.kit.kastel.vads.compiler.parser.symbol.Name;

public final class CallNode extends Node {

    public static final int SIDE_EFFECT = 0;
    public static final int ARGS_START = 1;

    private final Name calledFunctionName;

    public CallNode(Name calledFunctionName, Block block, Node sideEffect, Node[] args) {
        super(block, mergePredecessors(sideEffect, args));

        this.calledFunctionName = calledFunctionName;
    }

    @Override
    protected String info() {
        return this.calledFunctionName.asString();
    }

    private static List<Node> mergePredecessors(Node sideEffect, Node[] args) {
        List<Node> predecessors = new ArrayList<>(args.length + 1);
        predecessors.add(sideEffect);
        Collections.addAll(predecessors, args);
        return predecessors;
    }
}
