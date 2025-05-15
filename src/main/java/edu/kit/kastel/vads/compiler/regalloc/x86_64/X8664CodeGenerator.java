package edu.kit.kastel.vads.compiler.regalloc.x86_64;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.AddNode;
import edu.kit.kastel.vads.compiler.ir.node.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.DivNode;
import edu.kit.kastel.vads.compiler.ir.node.ModNode;
import edu.kit.kastel.vads.compiler.ir.node.MulNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.Phi;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;
import edu.kit.kastel.vads.compiler.ir.node.SubNode;
import edu.kit.kastel.vads.compiler.regalloc.IRegister;
import edu.kit.kastel.vads.compiler.regalloc.IRegisterAllocator;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public class X8664CodeGenerator {
    public String generateCode(List<IrGraph> program) {
        StringBuilder builder = new StringBuilder();
        for (IrGraph graph : program) {
            IRegisterAllocator allocator = new X8664RegisterAllocator(graph);
            Map<Node, IRegister> registers = allocator.allocateRegisters();
            generateForGraph(graph, builder, registers);
        }
        return builder.toString();
    }

    private void generateForGraph(IrGraph graph, StringBuilder builder, Map<Node, IRegister> registers) {
        Set<Node> visited = new HashSet<>();
        scan(graph.endBlock(), visited, builder, registers);
    }

    private void scan(Node node, Set<Node> visited, StringBuilder builder, Map<Node, IRegister> registers) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scan(predecessor, visited, builder, registers);
            }
        }

        switch (node) {
            case AddNode add -> defaultBinary(builder, registers, add, "addq");
            case SubNode sub -> defaultBinary(builder, registers, sub, "subq");
            case MulNode mul -> defaultBinary(builder, registers, mul, "imulq");
            case DivNode div -> divisionBinary(builder, registers, div);
            case ModNode mod -> divisionBinary(builder, registers, mod);
            case ReturnNode r -> ret(builder, registers, r);
            case ConstIntNode c -> move(builder, "$" + c.value(), registers.get(c));
            case Phi _ -> throw new UnsupportedOperationException("phi");
            case Block _,ProjNode _,StartNode _ -> {
                // do nothing, skip line break
                return;
            }
        }
        builder.append("\n");
    }

    private static void defaultBinary(
            StringBuilder builder,
            Map<Node, IRegister> registers,
            BinaryOperationNode node,
            String opcode) {
        move(builder,
                registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT)),
                registers.get(node));

        builder.append("\n").repeat(" ", 2)
                .append(opcode)
                .append(" ")
                .append(registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT)))
                .append(", ")
                .append(registers.get(node));
    }

    private static void divisionBinary(StringBuilder builder,
            Map<Node, IRegister> registers,
            BinaryOperationNode node) {
        move(builder,
                registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT)),
                X8664Register.RAX);

        builder.append("\n").repeat(" ", 2)
                .append("cqto");

        builder.append("\n").repeat(" ", 2)
                .append("idivq ")
                .append(registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT)))
                .append("\n");

        move(builder,
                // The quotient (needed for division) is in rax, the remainder (needed for
                // modulo) is in rdx
                node instanceof DivNode ? X8664Register.RAX : X8664Register.RDX,
                registers.get(node));
    }

    private static void move(
            StringBuilder builder,
            Object src,
            Object dest) {
        builder.repeat(" ", 2)
                .append("movq ")
                .append(src.toString())
                .append(", ")
                .append(dest.toString());
    }

    private static void ret(
            StringBuilder builder,
            Map<Node, IRegister> registers,
            ReturnNode node) {
        move(builder, registers.get(predecessorSkipProj(node, ReturnNode.RESULT)),
                X8664Register.RAX);
        builder.append("\n").repeat(" ", 2)
                .append("ret");
    }
}
