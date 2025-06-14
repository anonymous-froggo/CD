package edu.kit.kastel.vads.compiler.semantic.status;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.semantic.Namespace;

public class Scope extends Namespace<Scope.VariableStatus>{

    public Scope() {
        super();
    }

    private Scope(Map<Name, VariableStatus> content) {
        super(content);
    }

    @Override
    public Scope clone() {
        return new Scope(new HashMap<>(super.content));
    }

    public enum VariableStatus {

        DECLARED,
        INITIALIZED;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
