package edu.kit.kastel.vads.compiler.parser.ast;

//TODO: add if, while, for, continue, break
public sealed interface StatementTree extends Tree permits
    AssignmentTree,
    BlockTree,
    BreakTree,
    DeclarationTree,
    ReturnTree {
}
