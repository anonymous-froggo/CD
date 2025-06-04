package edu.kit.kastel.vads.compiler.codegen.x86_64;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.kit.kastel.vads.compiler.codegen.CodeGenerator;
import edu.kit.kastel.vads.compiler.codegen.Register;
import edu.kit.kastel.vads.compiler.codegen.RegisterAllocator;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.nodes.Block;
import edu.kit.kastel.vads.compiler.ir.nodes.BoolNode;
import edu.kit.kastel.vads.compiler.ir.nodes.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;
import edu.kit.kastel.vads.compiler.ir.nodes.Phi;
import edu.kit.kastel.vads.compiler.ir.nodes.ProjNode;
import edu.kit.kastel.vads.compiler.ir.nodes.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.nodes.StartNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.AddNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.DivNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.ModNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.MulNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.ShiftLeftNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.ShiftRightNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary_operation.SubNode;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public final class X8664CodeGenerator implements CodeGenerator {

    private final StringBuilder builder;

    private Map<Node, Register> registers;
    private int nStackRegisters;

    public X8664CodeGenerator() {
        this.builder = new StringBuilder();
    }

    @Override
    public String generateCode(List<IrGraph> program) {
        for (IrGraph graph : program) {
            X8664RegisterAllocator allocator = new X8664RegisterAllocator(graph);
            this.registers = allocator.allocateRegisters();
            this.nStackRegisters = allocator.numberOfStackRegisters();

            if (this.nStackRegisters > 0) {
                this.moveStackPointer(-this.nStackRegisters * X8664StackRegister.N_BYTES);
            }
            this.generateForGraph(graph);
        }

        return this.builder.toString();
    }

    @Override
    public String fromInt(int value) {
        return "$" + value;
    }

    @Override
    public String fromBoolean(boolean value) {
        return "$" + (value ? Integer.toHexString(0xFFFFFFFF) : Integer.toHexString(0x00000000));
    }

    private void generateForGraph(IrGraph graph) {
        Set<Node> visited = new HashSet<>();
        this.scan(graph.endBlock(), visited);
    }

    private void scan(Node node, Set<Node> visited) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                this.scan(predecessor, visited);
            }
        }

        switch (node) {
            // binary operation nodes
            case AddNode add -> this.defaultBinary(add, "addl");
            case DivNode div -> this.division(div);
            case ModNode mod -> this.division(mod);
            case MulNode mul -> this.defaultBinary(mul, "imull");
            case ShiftLeftNode shiftLeft -> this.shift(shiftLeft, "sall");
            case ShiftRightNode shiftRight -> this.shift(shiftRight, "sarl");
            case SubNode sub -> this.defaultBinary(sub, "subl");

            // other nodes
            case BoolNode bool -> this.constant(this.fromBoolean(bool.value()), this.registers.get(bool));
            case ConstIntNode constInt -> this.constant(this.fromInt(constInt.value()), this.registers.get(constInt));
            case ReturnNode ret -> this.ret(ret);
            case Phi _ -> throw new UnsupportedOperationException("phi");
            case Block _,ProjNode _,StartNode _ -> {
                // do nothing, skip line break
                return;
            }
        }
    }

    private void defaultBinary(BinaryOperationNode node, String opcode) {
        Register leftRegister = this.registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register rightRegister = this.registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        Register destRegister = this.registers.get(node);

        if (destRegister instanceof X8664StackRegister) {
            this.move(leftRegister, X8664Register.RAX);
            this.sourceDest(opcode, rightRegister, X8664Register.RAX);
            this.move(X8664Register.RAX, destRegister);
            return;
        }

        if (destRegister == leftRegister) {
            this.sourceDest(opcode, rightRegister, destRegister);
        } else if (destRegister == rightRegister) {
            if (node instanceof SubNode) {
                // This is of the form rightRegister = leftRegister - rightRegister, needs %eax
                // as temp register
                this.specialSub(leftRegister, destRegister);
                return;
            }

            this.sourceDest(opcode, leftRegister, destRegister);
        } else {
            this.move(leftRegister, destRegister);
            this.sourceDest(opcode, rightRegister, destRegister);
        }
    }

    private void shift(BinaryOperationNode node, String opcode) {
        Register srcRegister = this.registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register countRegister = this.registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        Register destRegister = this.registers.get(node);

        this.move(countRegister, X8664Register.RCX);

        if (destRegister instanceof X8664StackRegister) {
            this.move(srcRegister, X8664Register.RAX);
            this.builder.repeat(" ", 2)
                .append(opcode)
                .append(" ")
                .append(X8664Register.RCX.name(8))
                .append(", ")
                .append(X8664Register.RAX.name(32))
                .append("\n");
            this.move(X8664Register.RAX, destRegister);
            return;
        }

        if (srcRegister != destRegister) {
            this.move(srcRegister, destRegister);
        }

        this.builder.repeat(" ", 2)
            .append(opcode)
            .append(" ")
            .append(X8664Register.RCX.name(8))
            .append(", ")
            .append(destRegister.name(32))
            .append("\n");
    }

    private void specialSub(Register srcRegister, Register destRegister) {
        this.move(srcRegister, X8664Register.RAX);
        this.sourceDest("subl", destRegister, X8664Register.RAX);
        this.move(X8664Register.RAX, destRegister);
    }

    private void division(BinaryOperationNode node) {
        Register leftRegister = this.registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register rightRegister = this.registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        Register destRegister = this.registers.get(node);

        this.move(leftRegister, X8664Register.RAX);

        this.builder.repeat(" ", 2)
            .append("cdq")
            .append("\n");

        this.builder.repeat(" ", 2)
            .append("idivl ")
            .append(rightRegister.name(32))
            .append("\n");

        // The quotient (needed for division) is in rax,
        // the remainder (needed for modulo) is in rdx
        this.move(
            node instanceof DivNode ? X8664Register.RAX : X8664Register.RDX,
            destRegister
        );
    }

    private void ret(ReturnNode node) {
        this.move(
            this.registers.get(predecessorSkipProj(node, ReturnNode.RESULT)),
            X8664Register.RAX
        );

        if (this.nStackRegisters > 0) {
            this.moveStackPointer(this.nStackRegisters * 8);
        }

        this.builder.repeat(" ", 2)
            .append("ret")
            .append("\n");
    }

    private void move(Register srcRegister, Register destRegister) {
        this.builder.repeat(" ", 2)
            .append("movl ")
            .append(srcRegister.name(32))
            .append(", ")
            .append(destRegister.name(32))
            .append("\n");
    }

    private void constant(String constant, Register destRegister) {
        this.builder.repeat(" ", 2)
            .append("movl ")
            .append(constant)
            .append(", ")
            .append(destRegister.name(32))
            .append("\n");
    }

    private void sourceDest(String opcode, Register srcRegister, Register destRegister) {
        this.builder.repeat(" ", 2)
            .append(opcode)
            .append(" ")
            .append(srcRegister.name(32))
            .append(", ")
            .append(destRegister.name(32))
            .append("\n");
    }

    private void moveStackPointer(int offset) {
        this.builder.repeat(" ", 2)
            .append("add $")
            .append(offset)
            .append(", ")
            .append(X8664Register.RSP.name(64))
            .append("\n");
    }
}
