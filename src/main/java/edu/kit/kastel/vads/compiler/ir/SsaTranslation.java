package edu.kit.kastel.vads.compiler.ir;

import edu.kit.kastel.vads.compiler.ir.nodes.Block;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;
import edu.kit.kastel.vads.compiler.ir.nodes.ProjNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.DivNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.ModNode;
import edu.kit.kastel.vads.compiler.ir.nodes.control.ConditionalJumpNode;
import edu.kit.kastel.vads.compiler.ir.nodes.control.JumpNode;
import edu.kit.kastel.vads.compiler.ir.nodes.control.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.optimize.Optimizer;
import edu.kit.kastel.vads.compiler.ir.util.DebugInfo;
import edu.kit.kastel.vads.compiler.ir.util.DebugInfoHelper;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.LValueIdentifierTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.BoolTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.IdentifierTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.NumberLiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.UnaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ElseOptTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.WhileTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.function.BinaryOperator;

/// SSA translation as described in
/// [`Simple and Efficient Construction of Static Single Assignment Form`](https://compilers.cs.uni-saarland.de/papers/bbhlmz13cc.pdf).
///
/// This implementation also tracks side effect edges that can be used to avoid reordering of operations that cannot be
/// reordered.
///
/// We recommend to read the paper to better understand the mechanics implemented here.
public class SsaTranslation {

    // Input
    private final FunctionTree functionTree;

    // Output constructor
    private final GraphConstructor graphConstructor;

    private final Stack<StatementTree> loops = new Stack<>();
    private final Map<StatementTree, Set<JumpNode>> breakNodes = new HashMap<>();
    private final Map<StatementTree, Set<JumpNode>> continueNodes = new HashMap<>();

    public SsaTranslation(FunctionTree functionTree, Optimizer optimizer) {
        this.functionTree = functionTree;
        this.graphConstructor = new GraphConstructor(optimizer, functionTree.name().name().asString());
    }

    public IrGraph translate() {
        var visitor = new SsaTranslationVisitor();
        this.functionTree.accept(visitor, this);

        this.graphConstructor.calculateSchedule();

        return this.graphConstructor.graph();
    }

    private void writeVariable(Name variable, Block block, Node value) {
        this.graphConstructor.writeVariable(variable, block, value);
    }

    private Node readVariable(Name variable, Block block) {
        return this.graphConstructor.readVariable(variable, block);
    }

    private Block currentBlock() {
        return this.graphConstructor.currentBlock();
    }

    private void linkBreakNodes(Block followBlock) {
        StatementTree currentLoop = this.loops.peek();
        if (this.breakNodes.get(currentLoop) == null) {
            // No breaks in currentLoop
            return;
        }

        for (JumpNode breakNode : this.breakNodes.get(currentLoop)) {
            this.graphConstructor.link(breakNode, followBlock);
        }
    }

    private void linkContinueNodes(Block conditionBlock) {
        StatementTree currentLoop = this.loops.peek();
        if (this.continueNodes.get(currentLoop) == null) {
            // No coninues in currentLoop
            return;
        }

        for (JumpNode continueNode : this.continueNodes.get(currentLoop)) {
            this.graphConstructor.link(continueNode, conditionBlock);
        }
    }

    private static class SsaTranslationVisitor implements Visitor<SsaTranslation, Optional<Node>> {

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private static final Optional<Node> NOT_AN_EXPRESSION = Optional.empty();

        private final Deque<DebugInfo> debugStack = new ArrayDeque<>();

        private void pushSpan(Tree tree) {
            this.debugStack.push(DebugInfoHelper.debugInfo());
            DebugInfoHelper.setDebugInfo(new DebugInfo.SourceInfo(tree.span()));
        }

        private void popSpan() {
            DebugInfoHelper.setDebugInfo(this.debugStack.pop());
        }

        // Expression trees

        @Override
        public Optional<Node> visit(BinaryOperationTree binaryOperationTree, SsaTranslation data) {
            pushSpan(binaryOperationTree);

            Node lhs = binaryOperationTree.lhs().accept(this, data).orElseThrow();
            Node rhs = binaryOperationTree.rhs().accept(this, data).orElseThrow();
            Node res = switch (binaryOperationTree.operatorType()) {
                case MUL -> data.graphConstructor.newMul(lhs, rhs);
                case DIV -> projResultDivMod(data, data.graphConstructor.newDiv(lhs, rhs));
                case MOD -> projResultDivMod(data, data.graphConstructor.newMod(lhs, rhs));

                case PLUS -> data.graphConstructor.newAdd(lhs, rhs);
                case MINUS -> data.graphConstructor.newSub(lhs, rhs);

                case SHIFT_LEFT -> data.graphConstructor.newShiftLeft(lhs, rhs);
                case SHIFT_RIGHT -> data.graphConstructor.newShiftRight(lhs, rhs);

                case LESS_THAN -> data.graphConstructor.newLessThan(lhs, rhs);
                case LESS_THAN_EQ -> data.graphConstructor.newLessThanEq(lhs, rhs);
                case GREATER_THAN -> data.graphConstructor.newGreaterThan(lhs, rhs);
                case GREATER_THAN_EQ -> data.graphConstructor.newGreaterThanEq(lhs, rhs);

                case EQ -> data.graphConstructor.newEq(lhs, rhs);
                case NOT_EQ -> data.graphConstructor.newNotEq(lhs, rhs);

                case BITWISE_AND -> data.graphConstructor.newBitwiseAnd(lhs, rhs);

                case BITWISE_XOR -> data.graphConstructor.newBitwiseXor(lhs, rhs);

                case BITWISE_OR -> data.graphConstructor.newBitwiseOr(lhs, rhs);

                case LOGICAL_AND -> data.graphConstructor.newLogicalAnd(lhs, rhs);

                case LOGICAL_OR -> data.graphConstructor.newLogicalOr(lhs, rhs);
            };

            popSpan();

            return Optional.of(res);
        }

        @Override
        public Optional<Node> visit(BoolTree boolTree, SsaTranslation data) {
            pushSpan(boolTree);
            Node node = data.graphConstructor.newBoolNode(boolTree.value());
            popSpan();
            return Optional.of(node);
        }

        @Override
        public Optional<Node> visit(IdentifierTree identifierTree, SsaTranslation data) {
            pushSpan(identifierTree);
            Node value = data.readVariable(identifierTree.name().name(), data.currentBlock());
            popSpan();
            return Optional.of(value);
        }

        @Override
        public Optional<Node> visit(NumberLiteralTree numberLiteralTree, SsaTranslation data) {
            pushSpan(numberLiteralTree);
            Node node = data.graphConstructor.newConstInt((int) numberLiteralTree.parseValue().orElseThrow());
            popSpan();
            return Optional.of(node);
        }

        @Override
        public Optional<Node> visit(UnaryOperationTree unaryOperationTree, SsaTranslation data) {
            pushSpan(unaryOperationTree);

            Node input = unaryOperationTree.operand().accept(this, data).orElseThrow();
            Node res = switch (unaryOperationTree.operator().type()) {
                case BITWISE_NOT -> data.graphConstructor.newBitwiseNot(input);
                case LOGICAL_NOT -> data.graphConstructor.newLogicalNot(input);
                case NEGATE -> data.graphConstructor.newNegate(input);
            };

            popSpan();

            return Optional.of(res);
        }

        // Statement trees

        @Override
        public Optional<Node> visit(AssignmentTree assignmentTree, SsaTranslation data) {
            pushSpan(assignmentTree);
            BinaryOperator<Node> desugar = switch (assignmentTree.operatorType()) {
                case ASSIGN_PLUS -> data.graphConstructor::newAdd;
                case ASSIGN_MINUS -> data.graphConstructor::newSub;
                case ASSIGN_MUL -> data.graphConstructor::newMul;
                case ASSIGN_DIV -> (lhs, rhs) -> projResultDivMod(data, data.graphConstructor.newDiv(lhs, rhs));
                case ASSIGN_MOD -> (lhs, rhs) -> projResultDivMod(data, data.graphConstructor.newMod(lhs, rhs));
                case ASSIGN_AND -> data.graphConstructor::newBitwiseAnd;
                case ASSIGN_XOR -> data.graphConstructor::newBitwiseXor;
                case ASSIGN_OR -> data.graphConstructor::newBitwiseOr;
                case ASSIGN_SHIFT_LEFT -> data.graphConstructor::newShiftLeft;
                case ASSIGN_SHIFT_RIGHT -> data.graphConstructor::newShiftRight;
                case ASSIGN -> null;
            };

            switch (assignmentTree.lValue()) {
                case LValueIdentifierTree(var name) -> {
                    Node rhs = assignmentTree.expression().accept(this, data).orElseThrow();
                    if (desugar != null) {
                        rhs = desugar.apply(data.readVariable(name.name(), data.currentBlock()), rhs);
                    }
                    data.writeVariable(name.name(), data.currentBlock(), rhs);
                }
            }

            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(BlockTree blockTree, SsaTranslation data) {
            pushSpan(blockTree);
            for (StatementTree statement : blockTree.statements()) {
                statement.accept(this, data);
                // skip everything after a return in a block
                if (statement instanceof ReturnTree) {
                    break;
                }
            }
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(BreakTree breakTree, SsaTranslation data) {
            pushSpan(breakTree);

            // TODO this should maybe be checked in semantic analysis already, but it's also
            // convenient here, so idk
            if (data.loops.isEmpty()) {
                throw new SemanticException("break outside of loop at " + breakTree.span());
            }

            JumpNode breakNode = data.graphConstructor.newJump();

            StatementTree currentLoop = data.loops.peek();
            if (data.breakNodes.get(currentLoop) == null) {
                data.breakNodes.put(currentLoop, new HashSet<>());
            }

            data.breakNodes.get(currentLoop).add(breakNode);

            data.graphConstructor.newBlock();

            popSpan();

            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(ContinueTree continueTree, SsaTranslation data) {
            pushSpan(continueTree);

            // TODO this should maybe be checked in semantic analysis already, but it's also
            // convenient here, so idk
            if (data.loops.isEmpty()) {
                throw new SemanticException("continue outside of loop at " + continueTree.span());
            }

            JumpNode continueNode = data.graphConstructor.newJump();

            StatementTree currentLoop = data.loops.peek();
            if (data.continueNodes.get(currentLoop) == null) {
                data.continueNodes.put(currentLoop, new HashSet<>());
            }

            data.continueNodes.get(currentLoop).add(continueNode);

            data.graphConstructor.newBlock();

            popSpan();

            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(DeclarationTree declarationTree, SsaTranslation data) {
            pushSpan(declarationTree);
            if (declarationTree.initializer() != null) {
                Node rhs = declarationTree.initializer().accept(this, data).orElseThrow();
                data.writeVariable(declarationTree.name().name(), data.currentBlock(), rhs);
            }
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(ElseOptTree elseOptTree, SsaTranslation data) {
            pushSpan(elseOptTree);
            elseOptTree.elseStatement().accept(this, data);
            data.graphConstructor.jumpToNewBlock();
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(ForTree forTree, SsaTranslation data) {
            pushSpan(forTree);
            data.loops.push(forTree);

            if (forTree.initializer() != null) {
                forTree.initializer().accept(this, data);
            }

            // Don't seal conditionBlock yet, since goToCondition and continue nodes will
            // also link to it
            Block conditionBlock = data.graphConstructor.jumpToNewBlock();
            Node condition = forTree.condition().accept(this, data).orElseThrow();
            ConditionalJumpNode checkCondition = data.graphConstructor.newConditionalJump(condition);
            ProjNode projTrue = data.graphConstructor.newTrueProj(checkCondition);
            ProjNode projFalse = data.graphConstructor.newFalseProj(checkCondition);

            Block bodyBlock = data.graphConstructor.linkBranchToNewBlock(
                checkCondition, projTrue, ConditionalJumpNode.TRUE_TARGET
            );
            data.graphConstructor.sealBlock(bodyBlock);
            forTree.body().accept(this, data);

            if (forTree.postBody() != null) {
                Block postBodyBlock = data.graphConstructor.jumpToNewBlock();
                data.linkContinueNodes(postBodyBlock);
                data.graphConstructor.sealBlock(postBodyBlock);

                forTree.postBody().accept(this, data);
            } else {
                data.linkContinueNodes(conditionBlock);
            }

            data.graphConstructor.jumpToBlock(conditionBlock);
            data.graphConstructor.sealBlock(conditionBlock);

            Block followBlock = data.graphConstructor.linkBranchToNewBlock(
                checkCondition, projFalse, ConditionalJumpNode.FALSE_TARGET
            );
            data.linkBreakNodes(followBlock);
            data.graphConstructor.sealBlock(followBlock);

            data.loops.pop();
            popSpan();

            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(IfTree ifTree, SsaTranslation data) {
            pushSpan(ifTree);

            Node condition = ifTree.condition().accept(this, data).orElseThrow();
            ConditionalJumpNode checkCondition = data.graphConstructor.newConditionalJump(condition);
            ProjNode projTrue = data.graphConstructor.newTrueProj(checkCondition);
            ProjNode projFalse = data.graphConstructor.newFalseProj(checkCondition);

            Block thenBlock = data.graphConstructor.linkBranchToNewBlock(
                checkCondition, projTrue, ConditionalJumpNode.TRUE_TARGET
            );
            data.graphConstructor.sealBlock(thenBlock);
            ifTree.thenStatement().accept(this, data);

            if (ifTree.elseOpt() != null) {
                boolean thenNeedsExit = data.graphConstructor.currentBlockIsUsed();
                JumpNode exitThen = thenNeedsExit ? data.graphConstructor.newJump() : null;

                Block elseBlock = data.graphConstructor.linkBranchToNewBlock(
                    checkCondition, projFalse, ConditionalJumpNode.FALSE_TARGET
                );
                data.graphConstructor.sealBlock(elseBlock);
                ifTree.elseOpt().accept(this, data);

                // We only need a new block following the else if then needs an exit jump.
                // Otherwise we can just continue in the else block and seal it
                if (thenNeedsExit) {
                    data.graphConstructor.link(exitThen, data.currentBlock());
                }
                data.graphConstructor.sealBlock(data.currentBlock());
            } else {
                Block followBlock = data.graphConstructor.jumpToNewBlock();
                data.graphConstructor.linkBranch(
                    checkCondition, projFalse, ConditionalJumpNode.FALSE_TARGET, followBlock
                );
                data.graphConstructor.sealBlock(followBlock);
            }

            popSpan();

            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(ReturnTree returnTree, SsaTranslation data) {
            Block currentBlock = data.graphConstructor.currentBlock();
            if (currentBlock != data.graphConstructor.graph().startBlock() && currentBlock.predecessors().isEmpty()) {
                // Unreachable return, skip it
                return NOT_AN_EXPRESSION;
            }

            pushSpan(returnTree);

            Node result = returnTree.expression().accept(this, data).orElseThrow();
            ReturnNode ret = data.graphConstructor.newReturn(result);

            data.graphConstructor.link(ret, data.graphConstructor.graph().endBlock());

            // Create a new block with no predecessors
            data.graphConstructor.newBlock();

            popSpan();

            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(WhileTree whileTree, SsaTranslation data) {
            pushSpan(whileTree);
            data.loops.push(whileTree);

            // Don't seal conditionBlock yet, since goToCondition and continue nodes will
            // also link to it
            Block conditionBlock = data.graphConstructor.jumpToNewBlock();
            Node condition = whileTree.condition().accept(this, data).orElseThrow();
            ConditionalJumpNode checkCondition = data.graphConstructor.newConditionalJump(condition);
            ProjNode projTrue = data.graphConstructor.newTrueProj(checkCondition);
            ProjNode projFalse = data.graphConstructor.newFalseProj(checkCondition);

            Block bodyBlock = data.graphConstructor.linkBranchToNewBlock(
                checkCondition, projTrue, ConditionalJumpNode.TRUE_TARGET
            );
            data.graphConstructor.sealBlock(bodyBlock);
            whileTree.body().accept(this, data);

            data.graphConstructor.jumpToBlock(conditionBlock);
            data.linkContinueNodes(conditionBlock);
            data.graphConstructor.sealBlock(conditionBlock);

            Block followBlock = data.graphConstructor.linkBranchToNewBlock(
                checkCondition, projFalse, ConditionalJumpNode.FALSE_TARGET
            );
            data.linkBreakNodes(followBlock);
            data.graphConstructor.sealBlock(followBlock);

            data.loops.pop();
            popSpan();

            return NOT_AN_EXPRESSION;
        }

        // Other trees

        @Override
        public Optional<Node> visit(FunctionTree functionTree, SsaTranslation data) {
            pushSpan(functionTree);

            Node start = data.graphConstructor.newStart();
            data.graphConstructor.writeCurrentSideEffect(data.graphConstructor.newSideEffectProj(start));
            functionTree.body().accept(this, data);

            popSpan();

            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(LValueIdentifierTree lValueIdentTree, SsaTranslation data) {
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(NameTree nameTree, SsaTranslation data) {
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(ProgramTree programTree, SsaTranslation data) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Node> visit(TypeTree typeTree, SsaTranslation data) {
            throw new UnsupportedOperationException();
        }

        private Node projResultDivMod(SsaTranslation data, Node divMod) {
            // make sure we actually have a div or a mod, as optimizations could
            // have changed it to something else already
            if (!(divMod instanceof DivNode || divMod instanceof ModNode)) {
                return divMod;
            }
            Node projSideEffect = data.graphConstructor.newSideEffectProj(divMod);
            data.graphConstructor.writeCurrentSideEffect(projSideEffect);
            return data.graphConstructor.newResultProj(divMod);
        }
    }
}
