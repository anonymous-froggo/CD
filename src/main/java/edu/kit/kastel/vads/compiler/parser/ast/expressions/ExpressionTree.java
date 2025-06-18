package edu.kit.kastel.vads.compiler.parser.ast.expressions;

import edu.kit.kastel.vads.compiler.parser.ast.Tree;

public sealed interface ExpressionTree extends Tree permits
    BinaryOperationTree,
    BoolTree,
    CallTree,
    IdentTree,
    NumberLiteralTree,
    TernaryTree,
    UnaryOperationTree
{
}
