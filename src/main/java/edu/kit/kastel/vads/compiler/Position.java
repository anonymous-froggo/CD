package edu.kit.kastel.vads.compiler;

public sealed interface Position {

  int line();

  int column();

  public record SimplePosition(int line, int column) implements Position {

    @Override
    public String toString() {
      return (line() + 1) + ":" + (column() + 1);
    }
  }
}
