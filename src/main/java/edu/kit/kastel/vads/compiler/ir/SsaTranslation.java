package edu.kit.kastel.vads.compiler.ir;

import edu.kit.kastel.vads.compiler.ir.nodes.Block;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.DivNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.ModNode;
import edu.kit.kastel.vads.compiler.ir.nodes.control.ConditionalJumpNode;
import edu.kit.kastel.vads.compiler.ir.nodes.control.ControlFlowNode;
import edu.kit.kastel.vads.compiler.ir.nodes.control.JumpNode;
import edu.kit.kastel.vads.compiler.ir.nodes.control.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.optimize.Optimizer;
import edu.kit.kastel.vads.compiler.ir.util.DebugInfo;
import edu.kit.kastel.vads.compiler.ir.util.DebugInfoHelper;
import edu.kit.kastel.vads.compiler.parser.Printer;
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
import edu.kit.kastel.vads.compiler.parser.ast.statements.EmptyTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.WhileTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
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

    public SsaTranslation(FunctionTree functionTree, Optimizer optimizer) {
        this.functionTree = functionTree;
        this.graphConstructor = new GraphConstructor(optimizer, functionTree.name().name().asString());
    }

    public IrGraph translate() {
        var visitor = new SsaTranslationVisitor();
        this.functionTree.accept(visitor, this);

        this.graphConstructor.collectNodes();

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

        // TODO reorder methods according to package structure

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
                case

                    LValueIdentifierTree(var name) -> {
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
        public Optional<Node> visit(BoolTree boolTree, SsaTranslation data) {
            pushSpan(boolTree);
            Node node = data.graphConstructor.newBoolNode(boolTree.value());
            popSpan();
            return Optional.of(node);
        }

        @Override
        public Optional<Node> visit(BreakTree breakTree, SsaTranslation data) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'visit'");
        }

        @Override
        public Optional<Node> visit(ContinueTree continueTree, SsaTranslation data) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'visit'");
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
        public Optional<Node> visit(ForTree forTree, SsaTranslation data) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'visit'");
        }

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
        public Optional<Node> visit(IdentifierTree identifierTree, SsaTranslation data) {
            pushSpan(identifierTree);
            Node value = data.readVariable(identifierTree.name().name(), data.currentBlock());
            popSpan();
            return Optional.of(value);
        }

        @Override
        public Optional<Node> visit(IfTree ifTree, SsaTranslation data) {
            pushSpan(ifTree);

            boolean hasElse = !(ifTree.elseOpt() instanceof EmptyTree);

            Node condition = ifTree.condition().accept(this, data).orElseThrow();

            ControlFlowNode conditionalJump = data.graphConstructor.newConditionalJump(condition);
            Node projTrue = data.graphConstructor.newTrueProj(conditionalJump);
            Node projFalse = data.graphConstructor.newFalseProj(conditionalJump);

            Block thenBlock = data.graphConstructor.newBlock();
            // Link projTrue and thenBlock
            conditionalJump.setTarget(ConditionalJumpNode.TRUE_TARGET, thenBlock);
            thenBlock.addPredecessor(projTrue);
            // No more predecessors will be added, so seal
            data.graphConstructor.sealBlock(thenBlock);
            // Parse thenStatement
            ifTree.thenStatement().accept(this, data);

            // If the current block doesn't have predecessors it was created by a return, so
            // no new block and no jump is needed
            boolean needNewBlock = !data.graphConstructor.currentBlock().predecessors().isEmpty();
            // Insert a jump into the current block which will be linked to the block
            // following the if. This current block might be != thenBlock due to added
            // control flows in thenStatement.
            ControlFlowNode exitThen = needNewBlock ? data.graphConstructor.newJump() : null;

            if (hasElse) {
                // TODO
            } else {
                Block followBlock = needNewBlock ? data.graphConstructor.newBlock()
                    : data.graphConstructor.currentBlock();

                if (needNewBlock) {
                    // Link exitThen and followBlock
                    exitThen.setTarget(JumpNode.TARGET, followBlock);
                    followBlock.addPredecessor(exitThen);
                }

                // Link projFalse and followBlock
                conditionalJump.setTarget(ConditionalJumpNode.FALSE_TARGET, followBlock);
                followBlock.addPredecessor(projFalse);
                // No more predecessors will be added, so seal
                data.graphConstructor.sealBlock(followBlock);
            }

            popSpan();

            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(NumberLiteralTree numberLiteralTree, SsaTranslation data) {
            pushSpan(numberLiteralTree);
            Node node = data.graphConstructor.newConstInt((int) numberLiteralTree.parseValue().orElseThrow());
            popSpan();
            return Optional.of(node);
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
        public Optional<Node> visit(UnaryOperationTree negateTree, SsaTranslation data) {
            pushSpan(negateTree);
            Node node = negateTree.expression().accept(this, data).orElseThrow();
            Node res = data.graphConstructor.newSub(data.graphConstructor.newConstInt(0), node);
            popSpan();
            return Optional.of(res);
        }

        @Override
        public Optional<Node> visit(ProgramTree programTree, SsaTranslation data) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Node> visit(ReturnTree returnTree, SsaTranslation data) {
            pushSpan(returnTree);

            Node result = returnTree.expression().accept(this, data).orElseThrow();
            ControlFlowNode ret = data.graphConstructor.newReturn(result);

            // Link ret and endBlock
            ret.setTarget(ReturnNode.TARGET, data.graphConstructor.graph().endBlock());
            data.graphConstructor.graph().endBlock().addPredecessor(ret);

            // Create a new block with no predecessors but don't seal it,
            // since predecessors will be added
            data.graphConstructor.newBlock();

            popSpan();

            return NOT_AN_EXPRESSION;
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

        @Override
        public Optional<Node> visit(WhileTree whileTree, SsaTranslation data) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'visit'");
        }

        @Override
        public Optional<Node> visit(EmptyTree forTree, SsaTranslation data) {
            return NOT_AN_EXPRESSION;
        }
    }
}
