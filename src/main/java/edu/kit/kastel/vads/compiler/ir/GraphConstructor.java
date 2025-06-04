package edu.kit.kastel.vads.compiler.ir;

import edu.kit.kastel.vads.compiler.ir.nodes.Block;
import edu.kit.kastel.vads.compiler.ir.nodes.BoolNode;
import edu.kit.kastel.vads.compiler.ir.nodes.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;
import edu.kit.kastel.vads.compiler.ir.nodes.Phi;
import edu.kit.kastel.vads.compiler.ir.nodes.ProjNode;
import edu.kit.kastel.vads.compiler.ir.nodes.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.nodes.StartNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.AddNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.BitwiseAndNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.BitwiseOrNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.BitwiseXorNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.DivNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.EqNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.GreaterThanEqNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.GreaterThanNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.LessThanEqNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.LessThanNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.LogicalAndNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.LogicalOrNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.ModNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.MulNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.NotEqNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.ShiftLeftNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.ShiftRightNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.SubNode;
import edu.kit.kastel.vads.compiler.ir.nodes.unary_operation.BitwiseNotNode;
import edu.kit.kastel.vads.compiler.ir.nodes.unary_operation.LogicalNotNode;
import edu.kit.kastel.vads.compiler.ir.nodes.unary_operation.NegateNode;
import edu.kit.kastel.vads.compiler.ir.optimize.Optimizer;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;

import java.util.HashMap;
import java.util.HashSet;
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

    public GraphConstructor(Optimizer optimizer, String name) {
        this.optimizer = optimizer;
        this.graph = new IrGraph(name);
        this.currentBlock = this.graph.startBlock();
        // the start block never gets any more predecessors
        sealBlock(this.currentBlock);
    }

    public Node newStart() {
        assert currentBlock() == this.graph.startBlock() : "start must be in start block";
        return new StartNode(currentBlock());
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

    public Node newNotEqNode(Node left, Node right) {
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
        return this.optimizer.transform(new ConstIntNode(this.graph.startBlock(), value));
    }

    public Node newBoolNode(boolean value) {
        return this.optimizer.transform(new BoolNode(this.graph.startBlock(), value));
    }

    public Node newSideEffectProj(Node node) {
        return new ProjNode(currentBlock(), node, ProjNode.SimpleProjectionInfo.SIDE_EFFECT);
    }

    public Node newResultProj(Node node) {
        return new ProjNode(currentBlock(), node, ProjNode.SimpleProjectionInfo.RESULT);
    }

    public Node newReturn(Node result) {
        return new ReturnNode(currentBlock(), readCurrentSideEffect(), result);
    }

    public Block currentBlock() {
        return this.currentBlock;
    }

    public Phi newPhi() {
        // don't transform phi directly, it is not ready yet
        return new Phi(currentBlock());
    }

    public IrGraph graph() {
        return this.graph;
    }

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
            val = newPhi();
            this.incompletePhis.computeIfAbsent(block, _ -> new HashMap<>()).put(variable, (Phi) val);
        } else if (block.predecessors().size() == 1) {
            val = readVariable(variable, block.predecessors().getFirst().block());
        } else {
            val = newPhi();
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
            val = newPhi();
            Phi old = this.incompleteSideEffectPhis.put(block, (Phi) val);
            assert old == null : "double readSideEffectRecursive for " + block;
        } else if (block.predecessors().size() == 1) {
            val = readSideEffect(block.predecessors().getFirst().block());
        } else {
            val = newPhi();
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

}
