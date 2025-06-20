package edu.kit.kastel.vads.compiler.parser.symbol;

import edu.kit.kastel.vads.compiler.lexer.keywords.LibFunctionKeyword;

public record LibFunctionName(LibFunctionKeyword keyword) implements Name {

    @Override
    public String asString() {
        return keyword().asString();
    }

    // equals() and hashCode() need to be overridden because HashMaps don't work
    // otherwise. But I don't know why they don't.
    @Override
    public final boolean equals(Object obj) {
        if (!(obj instanceof LibFunctionName other)) {
            return false;
        }

        return other.getClass() == this.getClass()
                && other.keyword().type() == this.keyword().type();
    }

    @Override
    public final int hashCode() {
        return (keyword().type().hashCode()) ^ this.getClass().hashCode();
    }
}
