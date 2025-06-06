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
import edu.kit.kastel.vads.compiler.parser.ast.statements.EmptyTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.WhileTree;

public interface Visitor<T, R> {

    // Expressions

    R visit(BinaryOperationTree binaryOperationTree, T data);

    R visit(BoolTree trueTree, T data);

    R visit(IdentifierTree identExpressionTree, T data);

    R visit(NumberLiteralTree literalTree, T data);

    R visit(UnaryOperationTree negateTree, T data);

    // Statments

    R visit(AssignmentTree assignmentTree, T data);

    R visit(BlockTree blockTree, T data);

    R visit(BreakTree breakTree, T data);

    R visit(ContinueTree continueTree, T data);

    R visit(DeclarationTree declarationTree, T data);

    R visit(EmptyTree forTree, T data);

    R visit(ForTree forTree, T data);

    R visit(IfTree ifTree, T data);

    R visit(ReturnTree returnTree, T data);

    R visit(WhileTree whileTree, T data);

    // Others

    R visit(FunctionTree functionTree, T data);

    R visit(LValueIdentifierTree lValueIdentTree, T data);

    R visit(NameTree nameTree, T data);

    R visit(ProgramTree programTree, T data);

    R visit(TypeTree typeTree, T data);
}
