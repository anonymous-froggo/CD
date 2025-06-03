package edu.kit.kastel.vads.compiler.parser;

import edu.kit.kastel.vads.compiler.parser.ast.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.IdentifierTree;
import edu.kit.kastel.vads.compiler.parser.ast.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.LValueIdentifierTree;
import edu.kit.kastel.vads.compiler.parser.ast.NumberLiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.UnaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.WhileTree;
import edu.kit.kastel.vads.compiler.parser.ast.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.BoolTree;
import edu.kit.kastel.vads.compiler.parser.ast.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.FalseTree;
import edu.kit.kastel.vads.compiler.parser.ast.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;

import java.util.List;

/// This is a utility class to help with debugging the parser.
public class Printer {
    public static final int TAB_WIDTH = 4;

    private final Tree ast;
    private final StringBuilder builder = new StringBuilder();
    private boolean requiresIndent;
    private int indentDepth;

    public Printer(Tree ast) {
        this.ast = ast;
    }

    public static String print(Tree ast) {
        Printer printer = new Printer(ast);
        printer.printRoot();
        return printer.builder.toString();
    }

    private void printRoot() {
        printTree(this.ast);
    }

    private void printTree(Tree tree) {
        switch (tree) {
            case AssignmentTree(var lValue, var op, var expression) -> {
                printTree(lValue);
                space();
                this.builder.append(op);
                space();
                printTree(expression);
                semicolon();
            }
            case BinaryOperationTree(var lhs, var rhs, var op) -> {
                print("(");
                printTree(lhs);
                print(")");
                space();
                this.builder.append(op);
                space();
                print("(");
                printTree(rhs);
                print(")");
            }
            case BlockTree(List<StatementTree> statements, _) -> {
                print("{");
                lineBreak();
                this.indentDepth++;
                for (StatementTree statement : statements) {
                    printTree(statement);
                }
                this.indentDepth--;
                print("}");
            }
            case BreakTree(_) -> {
                print("break");
                semicolon();
            }
            case ContinueTree(_) -> {
                print("continue");
                semicolon();
            }
            case DeclarationTree(var type, var name, var initializer) -> {
                printTree(type);
                space();
                printTree(name);
                if (initializer != null) {
                    print(" = ");
                    printTree(initializer);
                }
                semicolon();
            }
            case FalseTree(_) -> print("false");
            case ForTree() -> throw new UnsupportedOperationException(
                "printing '" + tree.getClass() + "' is not implemented"
            );
            case FunctionTree(var returnType, var name, var body) -> {
                printTree(returnType);
                space();
                printTree(name);
                print("()");
                space();
                printTree(body);
            }
            case IdentifierTree(var name) -> printTree(name);
            case IfTree() -> throw new UnsupportedOperationException(
                "printing '" + tree.getClass() + "' is not implemented"
            );
            case LValueIdentifierTree(var name) -> printTree(name);
            case NameTree(var name, _) -> print(name.asString());
            case NumberLiteralTree(var value, _, _) -> this.builder.append(value);
            case ProgramTree(var topLevelTrees) -> {
                for (FunctionTree function : topLevelTrees) {
                    printTree(function);
                    lineBreak();
                }
            }
            case ReturnTree(var expr, _) -> {
                print("return ");
                printTree(expr);
                semicolon();
            }
            case BoolTree(_) -> print("true");
            case TypeTree(var type, _) -> print(type.asString());
            case UnaryOperationTree(var operator, var expression) -> {
                this.builder.append(operator);
                print("(");
                printTree(expression);
                print(")");
            }
            case WhileTree() -> throw new UnsupportedOperationException(
                "printing '" + tree.getClass() + "' is not implemented"
            );
        }
    }

    private void print(String str) {
        if (this.requiresIndent) {
            this.requiresIndent = false;
            this.builder.append(" ".repeat(TAB_WIDTH * this.indentDepth));
        }
        this.builder.append(str);
    }

    private void lineBreak() {
        this.builder.append("\n");
        this.requiresIndent = true;
    }

    private void semicolon() {
        this.builder.append(";");
        lineBreak();
    }

    private void space() {
        this.builder.append(" ");
    }

}
