package edu.kit.kastel.vads.compiler.semantic.visitor;

import edu.kit.kastel.vads.compiler.Visitor;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.BoolTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.IdentTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.NumberLiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.TernaryTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.UnaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.functions.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.lvalues.LValueIdentTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.DeclTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ElseOptTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.WhileTree;

/// A visitor that traverses a tree in postorder and lets [this{@link #visitor}] visit each node
/// @param <T> a type for additional data
/// @param <R> a type for a return type
public class RecursivePostorderVisitor<T, R> implements Visitor<T, R> {

    protected final Visitor<T, R> visitor;

    public RecursivePostorderVisitor(Visitor<T, R> visitor) {
        this.visitor = visitor;
    }

    // Expression trees

    @Override
    public R visit(BinaryOperationTree binaryOperationTree, T data) {
        R r = binaryOperationTree.lhs().accept(this, data);
        r = binaryOperationTree.rhs().accept(this, accumulate(data, r));
        r = this.visitor.visit(binaryOperationTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(BoolTree trueTree, T data) {
        R r = this.visitor.visit(trueTree, data);
        return r;
    }

    @Override
    public R visit(IdentTree indentifierTree, T data) {
        R r = indentifierTree.name().accept(this, data);
        r = this.visitor.visit(indentifierTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(NumberLiteralTree literalTree, T data) {
        R r = this.visitor.visit(literalTree, data);
        return r;
    }

    @Override
    public R visit(TernaryTree ternaryTree, T data) {
        R r = ternaryTree.condition().accept(this, data);
        r = ternaryTree.thenExpression().accept(this, accumulate(data, r));
        r = ternaryTree.elseExpression().accept(this, accumulate(data, r));
        r = this.visitor.visit(ternaryTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(UnaryOperationTree unaryOperationTree, T data) {
        R r = unaryOperationTree.operand().accept(this, data);
        r = this.visitor.visit(unaryOperationTree, accumulate(data, r));
        return r;
    }

    // Statement trees

    @Override
    public R visit(AssignmentTree assignmentTree, T data) {
        R r = assignmentTree.lValue().accept(this, data);
        r = assignmentTree.expression().accept(this, accumulate(data, r));
        r = this.visitor.visit(assignmentTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(BlockTree blockTree, T data) {
        R r;
        T d = data;
        for (StatementTree statement : blockTree.statements()) {
            r = statement.accept(this, d);
            d = accumulate(d, r);
        }
        r = this.visitor.visit(blockTree, d);
        return r;
    }

    @Override
    public R visit(BreakTree breakTree, T data) {
        R r = this.visitor.visit(breakTree, data);
        return r;
    }

    @Override
    public R visit(ContinueTree continueTree, T data) {
        R r = this.visitor.visit(continueTree, data);
        return r;
    }

    @Override
    public R visit(DeclTree declTree, T data) {
        R r = declTree.type().accept(this, data);
        r = declTree.name().accept(this, accumulate(data, r));
        if (declTree.initializer() != null) {
            r = declTree.initializer().accept(this, accumulate(data, r));
        }
        r = this.visitor.visit(declTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(ElseOptTree elseOptTree, T data) {
        R r = elseOptTree.elseStatement().accept(this, data);
        r = this.visitor.visit(elseOptTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(ForTree forTree, T data) {
        R r = null;
        if (forTree.initializer() != null) {
            r = forTree.initializer().accept(this, data);
        }
        r = forTree.condition().accept(this, accumulate(data, r));
        if (forTree.step() != null) {
            r = forTree.step().accept(this, accumulate(data, r));
        }
        r = forTree.body().accept(this, accumulate(data, r));
        r = this.visitor.visit(forTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(IfTree ifTree, T data) {
        R r = ifTree.condition().accept(this, data);
        r = ifTree.thenStatement().accept(this, accumulate(data, r));
        if (ifTree.elseOpt() != null) {
            r = ifTree.elseOpt().accept(this, accumulate(data, r));
        }
        r = this.visitor.visit(ifTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(ReturnTree returnTree, T data) {
        R r = returnTree.expression().accept(this, data);
        r = this.visitor.visit(returnTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(WhileTree whileTree, T data) {
        R r = whileTree.condition().accept(this, data);
        r = whileTree.body().accept(this, accumulate(data, r));
        r = this.visitor.visit(whileTree, accumulate(data, r));
        return r;
    }

    // Other trees

    @Override
    public R visit(FunctionTree functionTree, T data) {
        R r = functionTree.returnType().accept(this, data);
        r = functionTree.name().accept(this, accumulate(data, r));
        r = functionTree.body().accept(this, accumulate(data, r));
        r = this.visitor.visit(functionTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(LValueIdentTree lValueIdentTree, T data) {
        R r = lValueIdentTree.name().accept(this, data);
        r = this.visitor.visit(lValueIdentTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(NameTree nameTree, T data) {
        R r = this.visitor.visit(nameTree, data);
        return r;
    }

    @Override
    public R visit(ProgramTree programTree, T data) {
        R r;
        T d = data;
        for (FunctionTree tree : programTree.topLevelTrees()) {
            r = tree.accept(this, d);
            d = accumulate(d, r);
        }
        r = this.visitor.visit(programTree, d);
        return r;
    }

    @Override
    public R visit(TypeTree typeTree, T data) {
        R r = this.visitor.visit(typeTree, data);
        return r;
    }

    // Helper methods

    protected T accumulate(T data, R value) {
        return data;
    }
}
