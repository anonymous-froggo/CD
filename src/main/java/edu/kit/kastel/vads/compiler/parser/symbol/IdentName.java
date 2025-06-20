package edu.kit.kastel.vads.compiler.parser.symbol;
public record IdentName(String ident) implements Name {

    @Override
    public String asString() {
        return ident();
    }
}
