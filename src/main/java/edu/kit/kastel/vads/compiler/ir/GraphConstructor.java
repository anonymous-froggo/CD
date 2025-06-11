package edu.kit.kastel.vads.compiler.ir;

import edu.kit.kastel.vads.compiler.Main;
import edu.kit.kastel.vads.compiler.ir.nodes.Block;
import edu.kit.kastel.vads.compiler.ir.nodes.BoolNode;
import edu.kit.kastel.vads.compiler.ir.nodes.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;
import edu.kit.kastel.vads.compiler.ir.nodes.Phi;
import edu.kit.kastel.vads.compiler.ir.nodes.ProjNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.AddNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.BitwiseAndNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.BitwiseOrNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.BitwiseXorNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.DivNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.EqNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.GreaterThanEqNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.GreaterThanNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.LessThanEqNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.LessThanNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.LogicalAndNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.LogicalOrNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.ModNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.MulNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.NotEqNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.ShiftLeftNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.ShiftRightNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.SubNode;
import edu.kit.kastel.vads.compiler.ir.nodes.control.ConditionalJumpNode;
import edu.kit.kastel.vads.compiler.ir.nodes.control.ControlFlowNode;
import edu.kit.kastel.vads.compiler.ir.nodes.control.JumpNode;
import edu.kit.kastel.vads.compiler.ir.nodes.control.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.nodes.control.StartNode;
import edu.kit.kastel.vads.compiler.ir.nodes.unary.BitwiseNotNode;
import edu.kit.kastel.vads.compiler.ir.nodes.unary.LogicalNotNode;
import edu.kit.kastel.vads.compiler.ir.nodes.unary.NegateNode;
import edu.kit.kastel.vads.compiler.ir.optimize.Optimizer;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorsSkipProj;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class GraphConstructor {

    private final Optimizer optimizer;
    private final IrGraph graph;

    private final Map<Name, Map<Block, Node>> currentDef = new HashMap<>();

    private final Map<Block, Map<Name, Phi>> incompletePhis = new HashMap<>();

    private final Map<Block, Node> currentSideEffect = new HashMap<>();
    private final Map<Block, Phi> incompleteSideEffectPhis = new HashMap<>();

    private final Set<Block> sealedBlocks = new HashSet<>();
    private Block currentBlock;

    private final Map<Block, Set<Node>> collectedNodes = new HashMap<>();

    public GraphConstructor(Optimizer optimizer, String name) {
        this.optimizer = optimizer;
        this.graph = new IrGraph(name);
        this.currentBlock = graph().startBlock();

        // The start block never gets any more predecessors
        sealBlock(graph().startBlock());
    }

    public void collectNodes() {
        Set<Node> scanned = new HashSet<>();

        scanned.add(graph().endBlock());
        collectBlock(graph().endBlock(), scanned);
        graph().addBlock(graph().endBlock());

        for (Block block : graph().blocks()) {
            calculateSchedule(block);
        }

        for (Block block : graph().blocks()) {
            block.appendPhis();
            block.appendControlFlowExit();
        }
    }

    private void collectBlock(Block block, Set<Node> collected) {
        // Go through all of [block]'s control flow inputs
        for (Node controlFlowInput : predecessorsSkipProj(block)) {
            assert controlFlowInput instanceof ControlFlowNode
                : "Node " + controlFlowInput + " should be a control flow node";

            // Recursively collect [controlFlowInput]
            if (collected.add(controlFlowInput)) {
                collect(controlFlowInput, collected);
            }

            // Collect [controlFlowInput]'s block
            Block predecessorBlock = controlFlowInput.block();
            if (collected.add(predecessorBlock)) {
                collectBlock(predecessorBlock, collected);
                graph().addBlock(predecessorBlock);
            }
        }
    }

    private void collect(Node node, Set<Node> scanned) {
        Block block = node.block();

        if (node instanceof ControlFlowNode controlFlowNode && !(node instanceof StartNode)) {
            block.setControlFlowExit(controlFlowNode);
        } else {
            if (collectedNodes.get(block) == null) {
                collectedNodes.put(block, new HashSet<>());
            }
            collectedNodes.get(block).add(node);
        }

        for (Node predecessor : node.predecessors()) {
            if (scanned.add(predecessor)) {
                collect(predecessor, scanned);
            }
        }
    }

    private void calculateSchedule(Block block) {
        Set<Node> remainingNodes = collectedNodes.get(block);
        if (remainingNodes == null) {
            // Nothing to schedule
            return;
        }

        while (!remainingNodes.isEmpty()) {
            Node node = remainingNodes.iterator().next();
            remainingNodes.remove(node);
            scan(node, remainingNodes);
        }
    }

    private void scan(Node node, Set<Node> remainingNodes) {
        // Get topological order using DFS
        for (Node predecessor : node.predecessors()) {
            if (remainingNodes.remove(predecessor)) {
                scan(predecessor, remainingNodes);
            }
        }

        // Schedule node
        switch (node) {
            case StartNode _,ProjNode _,Block _ -> {
                // Do nothing
                break;
            }
            case Phi phi -> {
                if (phi.isSideEffectPhi()) {
                    // Side effect phis don't need to be scheduled
                    break;
                }

                List<Node> operands = predecessorsSkipProj(phi);
                // Add phi to the corresponding predecessor blocks
                for (int index = 0; index < operands.size(); index++) {
                    phi.block().predecessor(index).block().addPhi(phi, index);
                }
            }
            default -> {
                node.block().addToSchedule(node);
            }
        }
    }

    // Binary operation nodes

    public Node newAdd(Node left, Node right) {
        return this.optimizer.transform(new AddNode(currentBlock(), left, right));
    }

    public Node newBitwiseAnd(Node left, Node right) {
        return this.optimizer.transform(new BitwiseAndNode(currentBlock(), left, right));
    }

    public Node newBitwiseOr(Node left, Node right) {
        return this.optimizer.transform(new BitwiseOrNode(currentBlock(), left, right));
    }

    public Node newBitwiseXor(Node left, Node right) {
        return this.optimizer.transform(new BitwiseXorNode(currentBlock(), left, right));
    }

    public Node newDiv(Node left, Node right) {
        return this.optimizer.transform(new DivNode(currentBlock(), left, right, readCurrentSideEffect()));
    }

    public Node newEq(Node left, Node right) {
        return this.optimizer.transform(new EqNode(currentBlock(), left, right));
    }

    public Node newGreaterThanEq(Node left, Node right) {
        return this.optimizer.transform(new GreaterThanEqNode(currentBlock(), left, right));
    }

    public Node newGreaterThan(Node left, Node right) {
        return this.optimizer.transform(new GreaterThanNode(currentBlock(), left, right));
    }

    public Node newLessThanEq(Node left, Node right) {
        return this.optimizer.transform(new LessThanEqNode(currentBlock(), left, right));
    }

    public Node newLessThan(Node left, Node right) {
        return this.optimizer.transform(new LessThanNode(currentBlock(), left, right));
    }

    public Node newLogicalAnd(Node left, Node right) {
        return this.optimizer.transform(new LogicalAndNode(currentBlock(), left, right));
    }

    public Node newLogicalOr(Node left, Node right) {
        return this.optimizer.transform(new LogicalOrNode(currentBlock(), left, right));
    }

    public Node newMod(Node left, Node right) {
        return this.optimizer.transform(new ModNode(currentBlock(), left, right, readCurrentSideEffect()));
    }

    public Node newMul(Node left, Node right) {
        return this.optimizer.transform(new MulNode(currentBlock(), left, right));
    }

    public Node newNotEq(Node left, Node right) {
        return this.optimizer.transform(new NotEqNode(currentBlock(), left, right));
    }

    public Node newShiftLeft(Node left, Node right) {
        return this.optimizer.transform(new ShiftLeftNode(currentBlock(), left, right));
    }

    public Node newShiftRight(Node left, Node right) {
        return this.optimizer.transform(new ShiftRightNode(currentBlock(), left, right));
    }

    public Node newSub(Node left, Node right) {
        return this.optimizer.transform(new SubNode(currentBlock(), left, right));
    }

    // Unary operation nodes

    public Node newBitwiseNot(Node input) {
        return this.optimizer.transform(new BitwiseNotNode(currentBlock(), input));
    }

    public Node newLogicalNot(Node input) {
        return this.optimizer.transform(new LogicalNotNode(currentBlock(), input));
    }

    public Node newNegate(Node input) {
        return this.optimizer.transform(new NegateNode(currentBlock(), input));
    }

    // Other nodes

    public Node newConstInt(int value) {
        // always move const into start block, this allows better deduplication
        // and resultingly in better value numbering
        return this.optimizer.transform(new ConstIntNode(graph().startBlock(), value));
    }

    public Node newBoolNode(boolean value) {
        // always move const into start block, this allows better deduplication
        // and resultingly in better value numbering
        return this.optimizer.transform(new BoolNode(graph().startBlock(), value));
    }

    public Block newBlock() {
        Block block = new Block(graph());
        this.currentBlock = block;
        return block;
    }

    public ConditionalJumpNode newConditionalJump(Node condition) {
        return new ConditionalJumpNode(currentBlock(), condition);
    }

    public JumpNode newJump() {
        return new JumpNode(currentBlock());
    }

    public Phi newPhi() {
        // don't transform phi directly, it is not ready yet
        return new Phi(currentBlock());
    }

    public ProjNode newResultProj(Node node) {
        return new ProjNode(currentBlock(), node, ProjNode.SimpleProjectionInfo.RESULT);
    }

    public ProjNode newTrueProj(Node decision) {
        return new ProjNode(currentBlock(), decision, ProjNode.SimpleProjectionInfo.TRUE);
    }

    public ProjNode newFalseProj(Node decision) {
        return new ProjNode(currentBlock(), decision, ProjNode.SimpleProjectionInfo.FALSE);
    }

    public ProjNode newSideEffectProj(Node node) {
        return new ProjNode(currentBlock(), node, ProjNode.SimpleProjectionInfo.SIDE_EFFECT);
    }

    public ReturnNode newReturn(Node result) {
        return new ReturnNode(currentBlock(), readCurrentSideEffect(), result);
    }

    public StartNode newStart() {
        assert currentBlock() == this.graph.startBlock() : "start must be in start block";
        return new StartNode(currentBlock());
    }

    public boolean currentBlockIsUsed() {
        return !(currentBlock().isEmpty() && currentBlock().predecessors().isEmpty());
    }

    // Adds a jump from currentBlock to a new block and returns that block.
    public Block jumpToNewBlock() {
        if (!currentBlockIsUsed()) {
            // No jump and no new block needed
            return currentBlock();
        }

        JumpNode jump = newJump();
        Block newBlock = newBlock();
        link(jump, newBlock);

        return newBlock;
    }

    public void jumpToBlock(Block block) {
        if (!currentBlockIsUsed()) {
            // No jump needed
            return;
        }

        JumpNode jump = newJump();
        link(jump, block);
    }

    public Block linkBranchToNewBlock(ConditionalJumpNode conditionalJump, ProjNode branchProj, int idx) {
        // If currentBlock is empty and has no predecessors, no new block is needed
        Block newBlock = currentBlockIsUsed() ? newBlock() : currentBlock();
        linkBranch(conditionalJump, branchProj, idx, newBlock);

        return newBlock;
    }

    public void link(ControlFlowNode jump, Block block) {
        jump.setTarget(JumpNode.TARGET, block);
        block.addPredecessor(jump);
    }

    public void linkBranch(ConditionalJumpNode conditionalJump, ProjNode branchProj, int idx, Block block) {
        conditionalJump.setTarget(idx, block);
        block.addPredecessor(branchProj);
    }

    // Variable handling

    void writeVariable(Name variable, Block block, Node value) {
        this.currentDef.computeIfAbsent(variable, _ -> new HashMap<>()).put(block, value);
    }

    Node readVariable(Name variable, Block block) {
        Node node = this.currentDef.getOrDefault(variable, Map.of()).get(block);
        if (node != null) {
            return node;
        }
        return readVariableRecursive(variable, block);
    }

    private Node readVariableRecursive(Name variable, Block block) {
        Node val;
        if (!this.sealedBlocks.contains(block)) {
            val = new Phi(block);
            this.incompletePhis.computeIfAbsent(block, _ -> new HashMap<>()).put(variable, (Phi) val);
        } else if (block.predecessors().size() == 1) {
            val = readVariable(variable, block.predecessors().getFirst().block());
        } else {
            val = new Phi(block);
            writeVariable(variable, block, val);
            val = addPhiOperands(variable, (Phi) val);
        }
        writeVariable(variable, block, val);
        return val;
    }

    Node addPhiOperands(Name variable, Phi phi) {
        for (Node pred : phi.block().predecessors()) {
            phi.appendOperand(readVariable(variable, pred.block()));
        }
        return tryRemoveTrivialPhi(phi);
    }

    Node tryRemoveTrivialPhi(Phi phi) {
        // TODO: the paper shows how to remove trivial phis.
        // as this is not a problem in Lab 1 and it is just
        // a simplification, we recommend to implement this
        // part yourself.
        return phi;
    }

    void sealBlock(Block block) {
        for (Map.Entry<Name, Phi> entry : this.incompletePhis.getOrDefault(block, Map.of()).entrySet()) {
            addPhiOperands(entry.getKey(), entry.getValue());
        }
        Phi sideEffectPhi = this.incompleteSideEffectPhis.get(block);
        if (sideEffectPhi != null) {
            addPhiOperands(sideEffectPhi);
        }
        this.sealedBlocks.add(block);
    }

    public void writeCurrentSideEffect(Node node) {
        writeSideEffect(currentBlock(), node);
    }

    private void writeSideEffect(Block block, Node node) {
        this.currentSideEffect.put(block, node);
    }

    public Node readCurrentSideEffect() {
        return readSideEffect(currentBlock());
    }

    private Node readSideEffect(Block block) {
        Node node = this.currentSideEffect.get(block);
        if (node != null) {
            return node;
        }
        return readSideEffectRecursive(block);
    }

    private Node readSideEffectRecursive(Block block) {
        Node val;
        if (!this.sealedBlocks.contains(block)) {
            val = new Phi(block);
            Phi old = this.incompleteSideEffectPhis.put(block, (Phi) val);
            assert old == null : "double readSideEffectRecursive for " + block;
        } else if (block.predecessors().size() == 1) {
            val = readSideEffect(block.predecessors().getFirst().block());
        } else {
            val = new Phi(block);
            writeSideEffect(block, val);
            val = addPhiOperands((Phi) val);
        }
        writeSideEffect(block, val);
        return val;
    }

    Node addPhiOperands(Phi phi) {
        for (Node pred : phi.block().predecessors()) {
            phi.appendOperand(readSideEffect(pred.block()));
        }
        return tryRemoveTrivialPhi(phi);
    }

    // Getters/Setters

    public Block currentBlock() {
        return this.currentBlock;
    }

    public IrGraph graph() {
        return this.graph;
    }

    Set<Block> sealedBlocks() {
        return this.sealedBlocks;
    }
}
