package edu.kit.kastel.vads.compiler.parser.ast.lvalues;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.TreeVisitor;

public record LValueIdentTree(NameTree name) implements LValueTree {

    @Override
    public Span span() {
        return name().span();
    }

    @Override
    public <T, R> R accept(TreeVisitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
