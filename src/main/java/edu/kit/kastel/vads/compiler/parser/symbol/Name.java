package edu.kit.kastel.vads.compiler.parser.symbol;

import edu.kit.kastel.vads.compiler.lexer.Identifier;
import edu.kit.kastel.vads.compiler.lexer.Keyword;

public sealed interface Name permits IdentifierName, KeywordName {

    static Name forKeyword(Keyword keyword) {
        return new KeywordName(keyword.type());
    }

    static Name forIdentifier(Identifier identifier) {
        return new IdentifierName(identifier.value());
    }

    String asString();
}
