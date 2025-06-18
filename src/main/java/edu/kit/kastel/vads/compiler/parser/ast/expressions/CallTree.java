package edu.kit.kastel.vads.compiler.parser.ast.expressions;

import java.util.List;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.Visitor;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;

public record CallTree(NameTree functionName, List<ExpressionTree> args) implements ExpressionTree {

    public CallTree {
        args = List.copyOf(args);
    }

    @Override
    public Span span() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'span'");
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }
}
