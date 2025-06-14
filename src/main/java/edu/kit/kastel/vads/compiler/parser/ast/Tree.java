package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.Visitor;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.StatementTree;

public sealed interface Tree permits
    ExpressionTree,
    FunctionTree,
    LValueTree,
    NameTree,
    ProgramTree,
    StatementTree,
    TypeTree
{

    Span span();

    <T, R> R accept(Visitor<T, R> visitor, T data);
}
