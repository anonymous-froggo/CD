package edu.kit.kastel.vads.compiler.parser.ast.functions;

import java.util.ArrayList;
import java.util.List;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.lexer.keywords.LibFunctionKeyword;
import edu.kit.kastel.vads.compiler.lexer.keywords.LibFunctionKeyword.LibFunctionKeywordType;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.TreeVisitor;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.BlockTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;
import edu.kit.kastel.vads.compiler.parser.type.Type;

public record FunctionTree(TypeTree returnType, NameTree name, List<ParamTree> params, BlockTree body) implements Tree {

    // Lib functions
    public static FunctionTree FLUSH = libFunction(BasicType.INT, LibFunctionKeywordType.FLUSH, List.of());
    public static FunctionTree PRINT = libFunction(BasicType.INT, LibFunctionKeywordType.PRINT, List.of(BasicType.INT));
    public static FunctionTree READ = libFunction(BasicType.INT, LibFunctionKeywordType.READ, List.of());
    public static List<FunctionTree> LIB_FUNCTIONS = List.of(FLUSH, PRINT, READ);

    public FunctionTree {
        params = List.copyOf(params);
    }

    @Override
    public Span span() {
        return returnType().span().merge(body().span());
    }

    @Override
    public <T, R> R accept(TreeVisitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

    private static FunctionTree libFunction(
        Type returnType, LibFunctionKeywordType keywordType, List<Type> paramTypes
    ) {
        TypeTree returnTypeTree = new TypeTree(returnType, Span.NULL_SPAN);

        Name name = Name.forLibFunctionKeyword(new LibFunctionKeyword(keywordType, Span.NULL_SPAN));
        NameTree nameTree = new NameTree(name, Span.NULL_SPAN);

        List<ParamTree> paramTrees = new ArrayList<>();
        for (Type paramType : paramTypes) {
            TypeTree typeTree = new TypeTree(paramType, Span.NULL_SPAN);
            paramTrees.add(new ParamTree(typeTree, null));
        }

        return new FunctionTree(returnTypeTree, nameTree, paramTrees, null);
    }
}
