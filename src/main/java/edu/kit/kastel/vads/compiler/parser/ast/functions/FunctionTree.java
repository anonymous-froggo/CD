package edu.kit.kastel.vads.compiler.parser.ast.functions;

import java.util.List;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.Visitor;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.BlockTree;

public record FunctionTree(TypeTree returnType, NameTree name, List<ParamTree> params, BlockTree body) implements Tree {

    public FunctionTree {
        params = List.copyOf(params);
    }

    @Override
    public Span span() {
        return returnType().span().merge(body().span());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
