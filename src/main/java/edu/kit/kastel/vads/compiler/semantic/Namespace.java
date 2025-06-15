package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;

import java.util.HashMap;

import org.jspecify.annotations.Nullable;

public class Namespace<T> extends HashMap<Name, T> {

    public Namespace() {
        super();
    }

    public Namespace(int initialCapacity) {
        super(initialCapacity);
    }

    public @Nullable T put(NameTree name, T t) {
        return super.put(name.name(), t);
    }

    public @Nullable T get(NameTree name) {
        return super.get(name.name());
    }
}
