package edu.kit.kastel.vads.compiler.semantic.type;

import java.util.IdentityHashMap;
import java.util.Map;

import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.functions.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.functions.ParamTree;
import edu.kit.kastel.vads.compiler.parser.type.Type;
import edu.kit.kastel.vads.compiler.semantic.Namespace;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;
import edu.kit.kastel.vads.compiler.semantic.visitor.Scoper;

public class TypeScoper extends Scoper<Type> {

    private Map<ExpressionTree, Type> inferredTypes = new IdentityHashMap<>();

    private FunctionTree currentFunction = null;

    @Override
    protected Type cloneEntry(Type t) {
        // TODO refine this once more advanced types are supported
        return t;
    }

    @Override
    public void mergeScopeToCurrent(Namespace<Type> scope) {
        return;
    }

    @Override
    public Namespace<Type> intersectScopes(Namespace<Type> scope1, Namespace<Type> scope2) {
        return null;
    }

    @Override
    public void registerCurrentFunction(FunctionTree function) {
        currentFunction = function;

        for (ParamTree param : function.params()) {
            setType(param.name(), param.type().type());
        }
    }

    public Type getType(Tree tree) {
        return switch (tree) {
            case ExpressionTree expression -> this.inferredTypes.get(expression);
            case NameTree name -> currentScope().get(name);
            case TypeTree type -> type.type();
            default -> throw new IllegalArgumentException(tree + " cannot have a type associated to it.");
        };
    }

    public void setType(Tree tree, Type type) {
        switch (tree) {
            case ExpressionTree expression -> this.inferredTypes.put(expression, type);
            case NameTree name -> currentScope().put(name, type);
            default -> throw new IllegalArgumentException(tree + " cannot have a type associated to it.");
        }
    }

    public Type checkTypesEqual(Tree... trees) {
        Type prevType = null;
        Tree prevTree = null;
        for (Tree tree : trees) {
            Type type = getType(tree);
            if (prevType != null && prevType != type) {
                throw new SemanticException(
                    "Type mismatch: cannot convert from " + prevType + " to " + type + " at "
                        + prevTree.span().merge(tree.span())
                );
            }
            prevType = type;
            prevTree = tree;
        }

        return prevType;
    }

    public void checkTypesMatch(Type typeToMatch, Tree... trees) {
        for (Tree tree : trees) {
            Type type = getType(tree);
            if (type != typeToMatch) {
                throw new SemanticException(
                    "Type mismatch: cannot convert from " + type + " to " + typeToMatch + " at " + tree.span()
                );
            }
        }
    }

    public FunctionTree currentFunction() {
        return this.currentFunction;
    }
}
