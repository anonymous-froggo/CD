package edu.kit.kastel.vads.compiler.parser.symbol;

import edu.kit.kastel.vads.compiler.lexer.Ident;

public sealed interface Name permits IdentifierName {

    static Name forIdentifier(Ident identifier) {
        return new IdentifierName(identifier.value());
    }

    String asString();
}
