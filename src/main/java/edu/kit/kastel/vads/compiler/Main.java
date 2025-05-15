package edu.kit.kastel.vads.compiler;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.SsaTranslation;
import edu.kit.kastel.vads.compiler.ir.optimize.LocalValueNumbering;
import edu.kit.kastel.vads.compiler.lexer.Lexer;
import edu.kit.kastel.vads.compiler.parser.ParseException;
import edu.kit.kastel.vads.compiler.parser.Parser;
import edu.kit.kastel.vads.compiler.parser.TokenSource;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.regalloc.x86_64.X8664CodeGenerator;
import edu.kit.kastel.vads.compiler.semantic.SemanticAnalysis;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Invalid arguments: Expected one input file and one output file");
            System.exit(3);
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        ProgramTree program = lexAndParse(input);
        try {
            new SemanticAnalysis(program).analyze();
        } catch (SemanticException e) {
            e.printStackTrace();
            System.exit(7);
            return;
        }
        List<IrGraph> graphs = new ArrayList<>();
        for (FunctionTree function : program.topLevelTrees()) {
            SsaTranslation translation = new SsaTranslation(function, new LocalValueNumbering());
            graphs.add(translation.translate());
        }

        Path assembly = Path.of("assembly.S");
        String s = new X8664CodeGenerator().generateCode(graphs);
        Files.writeString(assembly, ".global main\n" + //
                ".global _main\n" + //
                ".text\n" + //
                "main:\n" + //
                "call _main\n" + //
                "# move the return value into the first argument for the syscall\n" + //
                "movq %rax, %rdi\n" + //
                "# move the exit syscall number into rax\n" + //
                "movq $0x3C, %rax\n" + //
                "syscall\n" + //
                "_main:\n" + //
                s);

        // Runtime.getRuntime()
        // .exec(String.format("/bin/sh -c \"gcc %s -o %s\"", assembly.toAbsolutePath(),
        // output.toAbsolutePath()));

        // String string;
        // Process process;
        // try {
        // process =
        Runtime.getRuntime().exec(String.format("gcc %s -o %s", assembly, output));
        // BufferedReader br = new BufferedReader(
        // new InputStreamReader(process.getInputStream()));
        // while ((string = br.readLine()) != null)
        // System.out.println("line: " + string);
        // process.waitFor();
        // System.out.println("exit: " + process.exitValue());
        // process.destroy();
        // } catch (Exception e) {
        // }
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
}