package edu.kit.kastel.vads.compiler.parser.symbol;

record IdentifierName(String identifier) implements Name {
    @Override
    public String asString() {
        return identifier();
    }
}
