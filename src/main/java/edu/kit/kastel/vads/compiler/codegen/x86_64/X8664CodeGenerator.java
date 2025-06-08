package edu.kit.kastel.vads.compiler.codegen.x86_64;

import java.util.List;
import java.util.Map;

import edu.kit.kastel.vads.compiler.codegen.CodeGenerator;
import edu.kit.kastel.vads.compiler.codegen.Register;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.nodes.Block;
import edu.kit.kastel.vads.compiler.ir.nodes.BoolNode;
import edu.kit.kastel.vads.compiler.ir.nodes.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;
import edu.kit.kastel.vads.compiler.ir.nodes.Phi;
import edu.kit.kastel.vads.compiler.ir.nodes.ProjNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.AddNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.BitwiseAndNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.BitwiseOrNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.BitwiseXorNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.DivNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.EqNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.GreaterThanEqNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.GreaterThanNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.LessThanEqNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.LessThanNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.LogicalAndNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.LogicalOrNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.ModNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.MulNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.ShiftLeftNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.ShiftRightNode;
import edu.kit.kastel.vads.compiler.ir.nodes.binary.SubNode;
import edu.kit.kastel.vads.compiler.ir.nodes.control.ConditionalJumpNode;
import edu.kit.kastel.vads.compiler.ir.nodes.control.JumpNode;
import edu.kit.kastel.vads.compiler.ir.nodes.control.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.nodes.control.StartNode;
import edu.kit.kastel.vads.compiler.ir.nodes.unary.BitwiseNotNode;
import edu.kit.kastel.vads.compiler.ir.nodes.unary.LogicalNotNode;
import edu.kit.kastel.vads.compiler.ir.nodes.unary.NegateNode;
import edu.kit.kastel.vads.compiler.ir.nodes.unary.UnaryOperationNode;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;
import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.successorsSkipProj;

public final class X8664CodeGenerator implements CodeGenerator {

    private final List<IrGraph> graphs;

    private final StringBuilder builder = new StringBuilder();

    private Map<Node, Register> registers;
    private int nStackRegisters;

    public X8664CodeGenerator(List<IrGraph> graphs) {
        this.graphs = graphs;
    }

    @Override
    public String generateCode() {
        for (IrGraph graph : this.graphs) {
            X8664RegisterAllocator allocator = new X8664RegisterAllocator(graph);
            this.registers = allocator.allocateRegisters();
            this.nStackRegisters = allocator.numberOfStackRegisters();

            if (this.nStackRegisters > 0) {
                moveStackPointer(-this.nStackRegisters * X8664StackRegister.N_BYTES);
            }
            generateForGraph(graph);
        }

        return this.builder.toString();
    }

    @Override
    public String fromInt(int value) {
        return "$" + value;
    }

    @Override
    public String fromBoolean(boolean value) {
        return "$0x" + Integer.toHexString(value ? 0xFF : 0);
    }

    private void generateForGraph(IrGraph graph) {
        for (Block block : graph.blocks()) {
            generateForBlock(block);
        }
    }

    private void generateForBlock(Block block) {
        this.builder.append(block.label())
            .append(":\n");

        for (Node node : block.nodes()) {
            // Generate block-local code
            generateForNode(node);
        }
    }

    private void generateForNode(Node node) {
        switch (node) {
            // Binary operation nodes
            case AddNode add -> defaultBinary(add, "addl");
            case BitwiseAndNode bitwiseAnd -> defaultBinary(bitwiseAnd, "andl");
            case BitwiseOrNode bitwiseOr -> defaultBinary(bitwiseOr, "orl");
            case BitwiseXorNode bitwiseXor -> defaultBinary(bitwiseXor, "xorl");
            case DivNode div -> division(div);
            case EqNode eq -> compare(eq, "sete");
            case GreaterThanEqNode greaterThanEq -> compare(greaterThanEq, "setge");
            case GreaterThanNode greaterThan -> compare(greaterThan, "setg");
            case LessThanEqNode lessThanEq -> compare(lessThanEq, "setle");
            case LessThanNode lessThan -> compare(lessThan, "setl");
            case LogicalAndNode logicalAnd -> defaultBinary(logicalAnd, "andl");
            case LogicalOrNode logicalOr -> defaultBinary(logicalOr, "orl");
            case ModNode mod -> division(mod);
            case MulNode mul -> defaultBinary(mul, "imull");
            case ShiftLeftNode shiftLeft -> shift(shiftLeft, "sall");
            case ShiftRightNode shiftRight -> shift(shiftRight, "sarl");
            case SubNode sub -> defaultBinary(sub, "subl");

            // Control flow nodes
            case ConditionalJumpNode conditionalJump -> {
                moveToPhis(conditionalJump.block());
                conditionalJump(conditionalJump);
            }
            case JumpNode jump -> {
                moveToPhis(jump.block());
                jump(jump);
            }
            case ReturnNode ret -> ret(ret);

            // Unary operation nodes
            case BitwiseNotNode bitwiseNot -> unary(bitwiseNot, "not", 8);
            case LogicalNotNode logicalNot -> unary(logicalNot, "not", 8);
            case NegateNode negate -> unary(negate, "neg", 32);

            // Other nodes
            case BoolNode bool -> constant(fromBoolean(bool.value()), this.registers.get(bool));
            case ConstIntNode constInt -> constant(fromInt(constInt.value()), this.registers.get(constInt));
            case Phi _ -> {
                // Nothing to do, since a phi's operands write into that phi's register on their
                // own
                return;
            }
            case Block _,ProjNode _,StartNode _ -> {
                // do nothing
                return;
            }
            default -> throw new UnsupportedOperationException(
                "code generation for " + node.getClass() + " not yet implemented"
            );
        }
    }

    private void moveToPhis(Block block) {
        for (Node node : block.nodes()) {
            // Move values into successor phis if they're not a side effect phi
            for (Node successor : successorsSkipProj(node)) {
                if (successor instanceof Phi phi && !phi.isSideEffectPhi()) {
                    move(this.registers.get(node), this.registers.get(successor));
                }
            }
        }
    }

    private void defaultBinary(BinaryOperationNode node, String opcode) {
        Register left = this.registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register right = this.registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        Register dest = this.registers.get(node);

        if (dest instanceof X8664StackRegister) {
            move(left, X8664Register.RAX);
            sourceDest(opcode, right, X8664Register.RAX);
            move(X8664Register.RAX, dest);
            return;
        }

        if (dest == left) {
            sourceDest(opcode, right, dest);
        } else if (dest == right) {
            if (node instanceof SubNode) {
                // This is of the form rightRegister = leftRegister - rightRegister, needs %eax
                // as temp register
                specialSub(left, dest);
                return;
            }

            sourceDest(opcode, left, dest);
        } else {
            move(left, dest);
            sourceDest(opcode, right, dest);
        }
    }

    private void unary(UnaryOperationNode node, String opcode, int bitlength) {
        Register input = this.registers.get(predecessorSkipProj(node, UnaryOperationNode.IN));
        Register dest = this.registers.get(node);

        if (dest instanceof X8664StackRegister) {
            move(input, X8664Register.RAX);

            this.builder.repeat(" ", 2)
                .append(opcode)
                .append(" ")
                .append(X8664Register.RAX.name(bitlength))
                .append("\n");

            move(X8664Register.RAX, dest);
            return;
        }

        move(input, dest);
        this.builder.repeat(" ", 2)
            .append(opcode)
            .append(" ")
            .append(dest.name(bitlength))
            .append("\n");
    }

    private void shift(BinaryOperationNode node, String opcode) {
        Register src = this.registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register count = this.registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        Register dest = this.registers.get(node);

        move(count, X8664Register.RCX);

        if (dest instanceof X8664StackRegister) {
            move(src, X8664Register.RAX);

            this.builder.repeat(" ", 2)
                .append(opcode)
                .append(" ")
                .append(X8664Register.RCX.name(8))
                .append(", ")
                .append(X8664Register.RAX.name(32))
                .append("\n");

            move(X8664Register.RAX, dest);
            return;
        }

        if (src != dest) {
            move(src, dest);
        }

        this.builder.repeat(" ", 2)
            .append(opcode)
            .append(" ")
            .append(X8664Register.RCX.name(8))
            .append(", ")
            .append(dest.name(32))
            .append("\n");
    }

    private void compare(BinaryOperationNode node, String opcode) {
        Register left = this.registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register right = this.registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        Register dest = this.registers.get(node);

        // Compare operands are switched in at&t for some reason
        this.builder.repeat(" ", 2)
            .append("cmp ")
            .append(right.name(32))
            .append(", ")
            .append(left.name(32))
            .append("\n");

        this.builder.repeat(" ", 2)
            .append(opcode)
            .append(" ")
            .append(dest.name(8))
            .append("\n");
    }

    private void specialSub(Register src, Register dest) {
        move(src, X8664Register.RAX);
        sourceDest("subl", dest, X8664Register.RAX);
        move(X8664Register.RAX, dest);
    }

    private void division(BinaryOperationNode node) {
        Register left = this.registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register right = this.registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        Register dest = this.registers.get(node);

        move(left, X8664Register.RAX);

        this.builder.repeat(" ", 2)
            .append("cdq")
            .append("\n");

        this.builder.repeat(" ", 2)
            .append("idivl ")
            .append(right.name(32))
            .append("\n");

        // The quotient (needed for division) is in rax,
        // the remainder (needed for modulo) is in rdx
        move(
            node instanceof DivNode ? X8664Register.RAX : X8664Register.RDX,
            dest
        );
    }

    // Control flow

    // TODO maybe change how conditions are handled -> actually leverage the
    // different jump instructions instead of calling test condition, condition
    private void conditionalJump(ConditionalJumpNode node) {
        Register condition = this.registers.get(predecessorSkipProj(node, ConditionalJumpNode.CONDITION));
        // Sets ZF to 0 if condition == false
        this.builder.repeat(" ", 2)
            .append("test ")
            .append(condition.name(8))
            .append(", ")
            .append(condition.name(8)).append("\n");

        // Activated when ZF != 0 (condition == true)
        this.builder.repeat(" ", 2)
            .append("jne ")
            .append(node.target(ConditionalJumpNode.TRUE_TARGET).label())
            .append("\n");

        // Activated when ZF == 0 (condition == false)
        this.builder.repeat(" ", 2)
            .append("jmp ")
            .append(node.target(ConditionalJumpNode.FALSE_TARGET).label())
            .append("\n");
    }

    private void jump(JumpNode jump) {
        this.builder.repeat(" ", 2)
            .append("jmp ")
            .append(jump.target(JumpNode.TARGET).label())
            .append("\n");
    }

    private void ret(ReturnNode node) {
        move(
            this.registers.get(predecessorSkipProj(node, ReturnNode.RESULT)),
            X8664Register.RAX
        );

        if (this.nStackRegisters > 0) {
            moveStackPointer(this.nStackRegisters * 8);
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
