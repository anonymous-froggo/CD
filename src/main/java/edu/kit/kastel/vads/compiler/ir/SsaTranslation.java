package edu.kit.kastel.vads.compiler.ir;

import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.binaryoperation.DivNode;
import edu.kit.kastel.vads.compiler.ir.node.binaryoperation.ModNode;
import edu.kit.kastel.vads.compiler.ir.optimize.Optimizer;
import edu.kit.kastel.vads.compiler.ir.util.DebugInfo;
import edu.kit.kastel.vads.compiler.ir.util.DebugInfoHelper;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.LValueIdentifierTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.StatementTree;
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
import edu.kit.kastel.vads.compiler.parser.ast.statements.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ReturnTree;
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
            BinaryOperator<Node> desugar = switch (assignmentTree.operator().type()) {
                case ASSIGN_MINUS -> data.graphConstructor::newSub;
                case ASSIGN_PLUS -> data.graphConstructor::newAdd;
                case ASSIGN_MUL -> data.graphConstructor::newMul;
                case ASSIGN_DIV -> (lhs, rhs) -> projResultDivMod(data, data.graphConstructor.newDiv(lhs, rhs));
                case ASSIGN_MOD -> (lhs, rhs) -> projResultDivMod(data, data.graphConstructor.newMod(lhs, rhs));
                case ASSIGN -> null;
                default -> throw new IllegalArgumentException(
                    "not an assignment operator " + assignmentTree.operator()
                );
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
                case MINUS -> data.graphConstructor.newSub(lhs, rhs);
                case PLUS -> data.graphConstructor.newAdd(lhs, rhs);
                case MUL -> data.graphConstructor.newMul(lhs, rhs);
                case DIV -> projResultDivMod(data, data.graphConstructor.newDiv(lhs, rhs));
                case MOD -> projResultDivMod(data, data.graphConstructor.newMod(lhs, rhs));
                default -> throw new IllegalArgumentException(
                    "not a binary expression operator " + binaryOperationTree.operatorType()
                );
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
            Node node = data.graphConstructor.newBooNode(boolTree.value());
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
        public Optional<Node> visit(IdentifierTree identExpressionTree, SsaTranslation data) {
            pushSpan(identExpressionTree);
            Node value = data.readVariable(identExpressionTree.name().name(), data.currentBlock());
            popSpan();
            return Optional.of(value);
        }

        @Override
        public Optional<Node> visit(IfTree ifTree, SsaTranslation data) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'visit'");
        }

        @Override
        public Optional<Node> visit(NumberLiteralTree literalTree, SsaTranslation data) {
            pushSpan(literalTree);
            Node node = data.graphConstructor.newConstInt((int) literalTree.parseValue().orElseThrow());
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
            Node node = returnTree.expression().accept(this, data).orElseThrow();
            Node ret = data.graphConstructor.newReturn(node);
            data.graphConstructor.graph().endBlock().addPredecessor(ret);
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

    }
}
