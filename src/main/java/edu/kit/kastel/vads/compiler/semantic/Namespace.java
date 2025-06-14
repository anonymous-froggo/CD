package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class Namespace<T> {

    protected final Map<Name, T> content;

    public Namespace() {
        this.content = new HashMap<>();
    }

    public Namespace(Map<Name, T> content) {
        this.content = content;
    }

    protected void put(NameTree name, T value) {
        this.content.put(name.name(), value);
    }

    protected @Nullable T get(NameTree name) {
        return this.content.get(name.name());
    }
}
