package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.functions.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.functions.ParamTree;
import edu.kit.kastel.vads.compiler.parser.ast.lvalues.LValueTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.StatementTree;

public sealed interface Tree permits
    ExpressionTree,
    FunctionTree,
    LValueTree,
    NameTree,
    ParamTree,
    ProgramTree,
    StatementTree,
    TypeTree
{

    Span span();

    <T, R> R accept(TreeVisitor<T, R> visitor, T data);
}
