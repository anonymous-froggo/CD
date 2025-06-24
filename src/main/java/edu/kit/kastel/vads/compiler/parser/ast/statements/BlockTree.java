package edu.kit.kastel.vads.compiler.parser.ast.statements;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.ast.TreeVisitor;

import java.util.List;

public record BlockTree(List<StatementTree> statements, Span span) implements StatementTree {

    public BlockTree {
        statements = List.copyOf(statements);
    }

    @Override
    public <T, R> R accept(TreeVisitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

    public static boolean skipsRemainingStatements(StatementTree statement) {
        return statement instanceof ReturnTree || statement instanceof BreakTree || statement instanceof ContinueTree;
    }
}
