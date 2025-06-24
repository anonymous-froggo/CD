package edu.kit.kastel.vads.compiler.parser.ast.functions;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.TreeVisitor;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;

public final record ParamTree(TypeTree type, NameTree name) implements Tree {

    @Override
    public Span span() {
        return type.span().merge(name.span());
    }

    @Override
    public <T, R> R accept(TreeVisitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

}
