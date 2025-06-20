package edu.kit.kastel.vads.compiler.parser;

import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.BoolTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.IdentExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.NumberLiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.TernaryTree;
import edu.kit.kastel.vads.compiler.parser.ast.expressions.UnaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.functions.CallTree;
import edu.kit.kastel.vads.compiler.parser.ast.functions.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.functions.ParamTree;
import edu.kit.kastel.vads.compiler.parser.ast.lvalues.LValueIdentTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.DeclTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ElseOptTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.statements.WhileTree;
import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
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
            // Expressions
            case BinaryOperationTree(var lhs, var rhs, var op) -> {
                print("(");
                printTree(lhs);
                print(")");
                space();
                this.builder.append(op.asString());
                space();
                print("(");
                printTree(rhs);
                print(")");
            }
            case BoolTree(var boolKeyword) -> builder.append(boolKeyword.asString());
            case CallTree(var ident, var args) -> {
                printTree(ident);
                print("(");
                printList(args, ", ");
                print(")");
            }
            case IdentExpressionTree(var name) -> printTree(name);
            case NumberLiteralTree(var value, _, _) -> this.builder.append(value);
            case TernaryTree(var condition, var thenExpression, var elseExpression) -> {
                print("(");
                printTree(condition);
                print(") ? (");
                printTree(thenExpression);
                print(") : (");
                printTree(elseExpression);
                print(")");
            }
            case UnaryOperationTree(var operator, var expression) -> {
                this.builder.append(operator.type());
                print("(");
                printTree(expression);
                print(")");
            }

            // Statements
            case AssignmentTree(var lValue, var op, var expression) -> {
                printTree(lValue);
                space();
                this.builder.append(op);
                space();
                printTree(expression);
                semicolon();
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
            case DeclTree(var type, var name, var initializer) -> {
                printTree(type);
                space();
                printTree(name);
                if (initializer != null) {
                    print(" = ");
                    printTree(initializer);
                }
                semicolon();
            }
            case ElseOptTree(var elseStatement, _) -> {
                print(" else ");
                printTree(elseStatement);
            }
            case ForTree(var initializer, var condition, var step, var body, _) -> {
                print("for (");
                if (initializer != null) {
                    printTree(initializer);
                }
                print("; ");
                printTree(condition);
                print("; ");
                if (step != null) {
                    printTree(step);
                }
                print(") ");
                lineBreak();
                printTree(body);
                lineBreak();
            }
            case IfTree(var condition, var thenStatement, var elseOpt, _) -> {
                print("if (");
                printTree(condition);
                print(") ");
                printTree(thenStatement);
                if (elseOpt != null) {
                    printTree(elseOpt);
                }
                // TODO maybe refine this line break as it generates an additional linebreak if
                // thenStatement is not a block
                lineBreak();
            }
            case ReturnTree(var expr, _) -> {
                print("return ");
                printTree(expr);
                semicolon();
            }
            case WhileTree(var condition, var statement, _) -> {
                print("while (");
                printTree(condition);
                print(") ");
                printTree(statement);
                lineBreak();
            }

            // Other trees
            case FunctionTree(var returnType, var name, var params, var body) -> {
                printTree(returnType);
                space();
                printTree(name);
                print("(");
                printList(params, ", ");
                print(") ");
                printTree(body);
                lineBreak();
            }
            case LValueIdentTree(var name) -> printTree(name);
            case NameTree(var name, _) -> print(name.asString());
            case ParamTree(var type, var name) -> {
                printTree(type);
                space();
                printTree(name);
            }
            case ProgramTree(var topLevelTrees, _) -> {
                for (FunctionTree function : topLevelTrees) {
                    printTree(function);
                    lineBreak();
                }
            }
            case TypeTree(var type, _) -> print(type.asString());
            default -> throw new UnsupportedOperationException("Printing " + tree + " not yet implemented");
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

    private void printList(List<? extends Tree> trees, String separator) {
        for (int i = 0; i < trees.size(); i++) {
            printTree(trees.get(i));
            if (i < trees.size() - 1) {
                print(separator);
            }
        }
    }
}
