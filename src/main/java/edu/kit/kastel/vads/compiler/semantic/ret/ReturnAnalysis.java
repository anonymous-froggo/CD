package edu.kit.kastel.vads.compiler.semantic.ret;

import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
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
import edu.kit.kastel.vads.compiler.semantic.SemanticException;
import edu.kit.kastel.vads.compiler.semantic.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.semantic.visitor.Unit;

/// Checks that functions return.
public class ReturnAnalysis implements NoOpVisitor<ReturnState> {

    // Statement trees

    @Override
    public Unit visit(AssignmentTree assignmentTree, ReturnState data) {
        // 𝐚𝐬𝐬𝐢𝐠𝐧(𝑥, 𝑒) does not return
        data.setReturns(assignmentTree, false);
        return NoOpVisitor.super.visit(assignmentTree, data);
    }

    @Override
    public Unit visit(BlockTree blockTree, ReturnState data) {
        // 𝐬𝐞𝐪(𝑠1, 𝑠2) returns if either 𝑠1 returns or 𝑠2 returns
        boolean returns = false;
        for (StatementTree statement : blockTree.statements()) {
            if (data.returns(statement)) {
                returns = true;
                break;
            }

            // Skip everything after a break or continue
            if (BlockTree.skipsRemainingStatements(statement)) {
                break;
            }
        }
        data.setReturns(blockTree, returns);

        return NoOpVisitor.super.visit(blockTree, data);
    }

    @Override
    public Unit visit(BreakTree breakTree, ReturnState data) {
        data.setReturns(breakTree, false);
        return NoOpVisitor.super.visit(breakTree, data);
    }

    @Override
    public Unit visit(ContinueTree continueTree, ReturnState data) {
        data.setReturns(continueTree, false);
        return NoOpVisitor.super.visit(continueTree, data);
    }

    @Override
    public Unit visit(DeclarationTree declarationTree, ReturnState data) {
        // 𝐝𝐞𝐜𝐥𝐚𝐫𝐞(𝑥, 𝜏 , 𝑠) returns if 𝑠 returns
        // For now, s can't return because it's an expression.
        data.setReturns(declarationTree, false);
        return NoOpVisitor.super.visit(declarationTree, data);
    }

    @Override
    public Unit visit(ElseOptTree elseOptTree, ReturnState data) {
        boolean returns = data.returns(elseOptTree.elseStatement());
        data.setReturns(elseOptTree, returns);

        return NoOpVisitor.super.visit(elseOptTree, data);
    }

    @Override
    public Unit visit(ForTree forTree, ReturnState data) {
        // 𝐟𝐨𝐫(𝑠1, 𝑒, 𝑠2, 𝑠3) does not return
        data.setReturns(forTree, false);
        return NoOpVisitor.super.visit(forTree, data);
    }

    @Override
    public Unit visit(IfTree ifTree, ReturnState data) {
        // 𝐢𝐟(𝑒, 𝑠1, 𝑠2) returns if both 𝑠1 and 𝑠2 return
        // If there is no else statement, ifTree doesn't return
        boolean returns = false;
        if (ifTree.elseOpt() != null) {
            returns = data.returns(ifTree.thenStatement())
                && data.returns(ifTree.elseOpt());
        }
        data.setReturns(ifTree, returns);

        return NoOpVisitor.super.visit(ifTree, data);
    }

    @Override
    public Unit visit(ReturnTree returnTree, ReturnState data) {
        data.setReturns(returnTree, true);
        return NoOpVisitor.super.visit(returnTree, data);
    }

    @Override
    public Unit visit(WhileTree whileTree, ReturnState data) {
        // 𝐰𝐡𝐢𝐥𝐞(𝑒, 𝑠) does not return
        data.setReturns(whileTree, false);
        return NoOpVisitor.super.visit(whileTree, data);
    }

    // Other trees

    @Override
    public Unit visit(FunctionTree functionTree, ReturnState data) {
        boolean returns = data.returns(functionTree.body());
        data.setReturns(functionTree, returns);

        if (!returns) {
            throw new SemanticException("function " + functionTree.name() + " does not return");
        }

        return NoOpVisitor.super.visit(functionTree, data);
    }
}
