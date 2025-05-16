package edu.kit.kastel.vads.compiler.codegen.x86_64;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.kit.kastel.vads.compiler.codegen.IRegister;
import edu.kit.kastel.vads.compiler.codegen.IRegisterAllocator;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.Phi;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;
import edu.kit.kastel.vads.compiler.ir.node.SubNode;
import edu.kit.kastel.vads.compiler.ir.node.binaryoperation.AddNode;
import edu.kit.kastel.vads.compiler.ir.node.binaryoperation.DivNode;
import edu.kit.kastel.vads.compiler.ir.node.binaryoperation.ModNode;
import edu.kit.kastel.vads.compiler.ir.node.binaryoperation.MulNode;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public class X8664CodeGenerator {
    public static String generateCode(List<IrGraph> program) {
        StringBuilder builder = new StringBuilder();
        for (IrGraph graph : program) {
            IRegisterAllocator allocator = new X8664RegisterAllocator(graph);
            Map<Node, IRegister> registerAllocation = allocator.allocateRegisters();
            generateForGraph(graph, builder, registerAllocation);
        }
        return builder.toString();
    }

    private static void generateForGraph(IrGraph graph, StringBuilder builder, Map<Node, IRegister> registers) {
        Set<Node> visited = new HashSet<>();
        scan(graph.endBlock(), visited, builder, registers);
    }

    private static void scan(Node node, Set<Node> visited, StringBuilder builder, Map<Node, IRegister> registers) {
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
            Map<Node, IRegister> registerAllocation,
            BinaryOperationNode node,
            String opcode) {
        IRegister leftRegister = registerAllocation.get(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        IRegister rightRegister = registerAllocation.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        IRegister destRegister = registerAllocation.get(node);

        if (destRegister == leftRegister) { // l = l - r
            sourceDest(builder, opcode, rightRegister, destRegister);
        } else if (destRegister == rightRegister) { // r = l - r
            if (!(node instanceof SubNode)) {
                sourceDest(builder, opcode, leftRegister, destRegister);
                return;
            }
            // This is of the form rightRegister = leftRegister - rightRegister            
            sourceDest(builder, opcode, leftRegister, destRegister);    
        } else { // d = l - r
            move(builder, leftRegister, destRegister);
            sourceDest(builder, opcode, rightRegister, destRegister);
        }
    }

    private static void divisionBinary(StringBuilder builder,
            Map<Node, IRegister> registerAllocation,
            BinaryOperationNode node) {
        IRegister leftRegister = registerAllocation.get(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        IRegister rightRegister = registerAllocation.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        IRegister destRegister = registerAllocation.get(node);

        move(builder, leftRegister, X8664Register.RAX);

        builder.append("\n").repeat(" ", 2)
                .append("cqto");

        builder.append("\n").repeat(" ", 2)
                .append("idivq ")
                .append(rightRegister)
                .append("\n");

        // The quotient (needed for division) is in rax,
        // the remainder (needed for modulo) is in rdx
        move(builder, node instanceof DivNode ? X8664Register.RAX : X8664Register.RDX, destRegister);
    }

    private static void ret(
            StringBuilder builder,
            Map<Node, IRegister> registerAllocation,
            ReturnNode node) {
        move(builder,
                registerAllocation.get(predecessorSkipProj(node, ReturnNode.RESULT)),
                X8664Register.RAX);

        builder.append("\n").repeat(" ", 2)
                .append("ret");
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

    private static void sourceDest(StringBuilder builder, String opcode, IRegister sourceRegister,
            IRegister destRegister) {
        builder.append("\n").repeat(" ", 2)
                .append(opcode)
                .append(" ")
                .append(sourceRegister)
                .append(", ")
                .append(destRegister);
    }
}
