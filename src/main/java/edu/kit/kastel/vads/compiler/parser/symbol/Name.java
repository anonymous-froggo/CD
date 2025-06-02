package edu.kit.kastel.vads.compiler.parser.symbol;

import edu.kit.kastel.vads.compiler.lexer.Identifier;

public sealed interface Name permits IdentifierName {

    static Name forIdentifier(Identifier identifier) {
        return new IdentifierName(identifier.value());
    }

    String asString();
}
