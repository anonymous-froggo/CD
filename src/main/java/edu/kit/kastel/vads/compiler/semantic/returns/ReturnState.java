package edu.kit.kastel.vads.compiler.semantic.returns;

import java.util.HashMap;
import java.util.Map;

import edu.kit.kastel.vads.compiler.parser.ast.Tree;

public class ReturnState {

    private final Map<Tree, Boolean> returns = new HashMap<>();

    public boolean returns(Tree tree) {
        Boolean result = returns.get(tree);
        if (result == null) {
            throw new IllegalArgumentException("There is no 'returns' value assigned to " + tree);
        }

        return result;
    }

    public boolean setReturns(Tree tree, boolean returns) {
        if (this.returns.put(tree, returns) != null) {
            /// {tree} has been assigned a returns twice, this shouldn't happen
            throw new IllegalArgumentException(tree + " has already been assigned a 'returns' value");
        }

        return returns;
    }
}
