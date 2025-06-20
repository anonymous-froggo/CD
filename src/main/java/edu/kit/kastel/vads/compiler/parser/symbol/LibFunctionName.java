package edu.kit.kastel.vads.compiler.parser.symbol;

import edu.kit.kastel.vads.compiler.lexer.keywords.LibFunctionKeyword;

public record LibFunctionName(LibFunctionKeyword keyword) implements Name {

    @Override
    public String asString() {
        return keyword().asString();
    }
}
