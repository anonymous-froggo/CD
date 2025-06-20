package edu.kit.kastel.vads.compiler;

import edu.kit.kastel.vads.compiler.codegen.CodeGenerator;
import edu.kit.kastel.vads.compiler.codegen.x86_64.X8664CodeGenerator;
import edu.kit.kastel.vads.compiler.ir.SsaGraph;
import edu.kit.kastel.vads.compiler.ir.SsaTranslation;
import edu.kit.kastel.vads.compiler.ir.optimize.LocalValueNumbering;
import edu.kit.kastel.vads.compiler.ir.util.YCompPrinter;
import edu.kit.kastel.vads.compiler.lexer.Lexer;
import edu.kit.kastel.vads.compiler.parser.ParseException;
import edu.kit.kastel.vads.compiler.parser.Parser;
import edu.kit.kastel.vads.compiler.parser.Printer;
import edu.kit.kastel.vads.compiler.parser.TokenSource;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.functions.FunctionTree;
import edu.kit.kastel.vads.compiler.semantic.SemanticAnalysis;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static boolean DEBUG = false;

    public static final Path OUTPUT_FOLDER = Path.of("output");
    private static final Path ASSEMBLY_FILE = Path.of("assembly.S");
    private static final Path GRAPHS_FOLDER = OUTPUT_FOLDER.toAbsolutePath().resolve("graphs");

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Invalid arguments: Expected one input file and one output file");
            System.exit(3);
        }
        if (args.length > 2 && args[2].equals("-d")) {
            DEBUG = true;
        }

        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        ProgramTree programTree = lexAndParse(input);
        if (DEBUG) {
            System.out.println(Printer.print(programTree));
        }

        try {
            new SemanticAnalysis(programTree).analyze();
        } catch (SemanticException e) {
            e.printStackTrace();
            System.exit(7);
            return;
        }

        List<SsaGraph> graphs = new ArrayList<>();
        try {
            for (FunctionTree function : programTree.topLevelTrees()) {
                SsaTranslation ssaTranslation = new SsaTranslation(function, new LocalValueNumbering());
                graphs.add(ssaTranslation.translate());
            }
        } catch (SemanticException e) {
            // TODO this is only temporary, because breaks and continues are checked for
            // during SSA translation
            e.printStackTrace();
            System.exit(7);
            return;
        }

        if (DEBUG) {
            for (SsaGraph graph : graphs) {
                dumpGraph(graph, "before-codegen");
            }
        }

        CodeGenerator codeGenerator = new X8664CodeGenerator(graphs);
        String generatedCode = codeGenerator.generateCode();

        assembleAndLink(generatedCode, output);
    }

    private static ProgramTree lexAndParse(Path input) throws IOException {
        try {
            Lexer lexer = Lexer.forString(Files.readString(input));
            TokenSource tokenSource = new TokenSource(lexer);
            Parser parser = new Parser(tokenSource);
            return parser.parseProgram();
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(42);
            throw new AssertionError("unreachable");
        }
    }

    private static void assembleAndLink(String generatedCode, Path output) throws IOException {
        Path assemblyPath = output.toAbsolutePath().resolveSibling(ASSEMBLY_FILE);

        Files.writeString(
            assemblyPath,
                ".global main\n" +
                        ".global _main\n" +
                        ".text\n" +
                        "main:\n" +
                        "call _main\n" +
                        "movq %rax, %rdi\n" +
                        "movq $0x3C, %rax\n" +
                        "syscall\n" +
                        "_main:\n" +
                        generatedCode);

        try {
            Process gccProcess = Runtime.getRuntime().exec(new String[] {
                    "gcc", assemblyPath.toString(), "-o", output.toString()
            });
            gccProcess.waitFor();

            if (Main.DEBUG) {
                System.out.println("gcc exited with code " + gccProcess.exitValue());

                Process outputProcess = Runtime.getRuntime().exec(new String[] {
                        "./" + output.toString()
                });
                outputProcess.waitFor();

                System.out.println("output exited with code " + outputProcess.exitValue());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void dumpGraph(SsaGraph graph, String key) throws IOException {
        Files.writeString(
                GRAPHS_FOLDER.toAbsolutePath().resolve(graph.name() + "-" + key + ".vcg"),
                YCompPrinter.print(graph));
    }
}
