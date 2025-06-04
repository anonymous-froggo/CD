package edu.kit.kastel.vads.compiler.parser.visitor;

import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.LValueIdentifierTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
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
import edu.kit.kastel.vads.compiler.parser.ast.statements.EmptyTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.WhileTree;

/// A visitor that does nothing and returns [Unit#INSTANCE] by default.
/// This can be used to implement operations only for specific tree types.
public interface NoOpVisitor<T> extends Visitor<T, Unit> {

    @Override
    default Unit visit(AssignmentTree assignmentTree, T data) {
        return Unit.INSTANCE;
    }

    @Override
    default Unit visit(BinaryOperationTree binaryOperationTree, T data) {
        return Unit.INSTANCE;
    }

    @Override
    default Unit visit(BlockTree blockTree, T data) {
        return Unit.INSTANCE;
    }

    @Override
    default Unit visit(BoolTree trueTree, T data) {
        return Unit.INSTANCE;
    }

    @Override
    default Unit visit(BreakTree breakTree, T data) {
        return Unit.INSTANCE;
    }

    @Override
    default Unit visit(ContinueTree continueTree, T data) {
        return Unit.INSTANCE;
    }

    @Override
    default Unit visit(DeclarationTree declarationTree, T data) {
        return Unit.INSTANCE;
    }

    @Override
    default Unit visit(ElseOptTree elseOptTree, T data) {
        return Unit.INSTANCE;
    }

    @Override
    default Unit visit(EmptyTree forTree, T data) {
        return Unit.INSTANCE;
    }

    @Override
    default Unit visit(ForTree forTree, T data) {
        return Unit.INSTANCE;
    }

    @Override
    default Unit visit(FunctionTree functionTree, T data) {
        return Unit.INSTANCE;
    }

    @Override
    default Unit visit(IdentifierTree identExpressionTree, T data) {
        return Unit.INSTANCE;
    }

    @Override
    default Unit visit(IfTree ifTree, T data) {
        return Unit.INSTANCE;
    }

    @Override
    default Unit visit(NumberLiteralTree literalTree, T data) {
        return Unit.INSTANCE;
    }

    @Override
    default Unit visit(LValueIdentifierTree lValueIdentTree, T data) {
        return Unit.INSTANCE;
    }

    @Override
    default Unit visit(NameTree nameTree, T data) {
        return Unit.INSTANCE;
    }

    @Override
    default Unit visit(UnaryOperationTree negateTree, T data) {
        return Unit.INSTANCE;
    }

    @Override
    default Unit visit(ProgramTree programTree, T data) {
        return Unit.INSTANCE;
    }

    @Override
    default Unit visit(ReturnTree returnTree, T data) {
        return Unit.INSTANCE;
    }

    @Override
    default Unit visit(TypeTree typeTree, T data) {
        return Unit.INSTANCE;
    }

    @Override
    default Unit visit(WhileTree whileTree, T data) {
        return Unit.INSTANCE;
    }
}
