package edu.kit.kastel.vads.compiler.parser.ast.functions;

import java.util.List;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.Visitor;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.StatementTree;
import edu.kit.kastel.vads.compiler.parser.symbol.LibFunctionName;

public record CallTree(NameTree functionName, List<ExpressionTree> args) implements ExpressionTree, StatementTree {

    public CallTree {
        args = List.copyOf(args);
    }

    @Override
    public Span span() {
        if (args.isEmpty()) {
            return functionName().span();
        }
        
        return functionName().span().merge(args.getLast().span());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

    public boolean isLibFunctionCall() {
        return functionName().name() instanceof LibFunctionName;
    }
}
