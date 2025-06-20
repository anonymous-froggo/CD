package edu.kit.kastel.vads.compiler.parser.ast.statements;

import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.functions.CallTree;

public sealed interface StatementTree extends Tree permits
    AssignmentTree,
    BlockTree,
    BreakTree,
    CallTree,
    ContinueTree,
    DeclTree,
    ElseOptTree,
    ForTree,
    IfTree,
    ReturnTree,
    WhileTree
{
}
