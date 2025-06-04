package edu.kit.kastel.vads.compiler.parser.ast.statements;

import edu.kit.kastel.vads.compiler.parser.ast.Tree;

public sealed interface StatementTree extends Tree permits
    AssignmentTree,
    BlockTree,
    BreakTree,
    ContinueTree,
    DeclarationTree,
    ElseOptTree,
    EmptyTree,
    ForTree,
    IfTree,
    ReturnTree,
    WhileTree
{
}
