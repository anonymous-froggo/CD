package edu.kit.kastel.vads.compiler.parser.symbol;

import edu.kit.kastel.vads.compiler.lexer.Ident;
import edu.kit.kastel.vads.compiler.lexer.keywords.LibFunctionKeyword;

public sealed interface Name permits IdentName, LibFunctionName {

    public static Name forIdent(Ident ident) {
        return new IdentName(ident.value());
    }

    public static Name forLibFunctionKeyword(LibFunctionKeyword keyword) {
        return new LibFunctionName(keyword);
    }

    String asString();
}
