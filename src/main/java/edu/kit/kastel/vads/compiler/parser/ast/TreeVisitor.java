package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.parser.ast.expressions.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.BoolTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.IdentExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.NumberLiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.TernaryTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.UnaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.functions.CallTree;
import edu.kit.kastel.vads.compiler.parser.ast.functions.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.functions.ParamTree;
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
import edu.kit.kastel.vads.compiler.parser.ast.statements.WhileTree;

public interface TreeVisitor<T, R> {

    // Expression trees

    R visit(BinaryOperationTree binaryOperationTree, T data);

    R visit(BoolTree trueTree, T data);

    R visit(IdentExpressionTree identExpressionTree, T data);

    R visit(NumberLiteralTree literalTree, T data);

    R visit(TernaryTree ternaryTree, T data);

    R visit(UnaryOperationTree unaryOperationTree, T data);

    // Functions

    R visit(CallTree callTree, T data);

    R visit(FunctionTree functionTree, T data);

    R visit(ParamTree paramTree, T data);

    // Statement trees

    R visit(AssignmentTree assignmentTree, T data);

    R visit(BlockTree blockTree, T data);

    R visit(BreakTree breakTree, T data);

    R visit(ContinueTree continueTree, T data);

    R visit(DeclTree declTree, T data);

    R visit(ElseOptTree elseOptTree, T data);

    R visit(ForTree forTree, T data);

    R visit(IfTree ifTree, T data);

    R visit(ReturnTree returnTree, T data);

    R visit(WhileTree whileTree, T data);

    // Other trees

    R visit(LValueIdentTree lValueIdentTree, T data);

    R visit(NameTree nameTree, T data);

    R visit(ProgramTree programTree, T data);

    R visit(TypeTree typeTree, T data);
}
