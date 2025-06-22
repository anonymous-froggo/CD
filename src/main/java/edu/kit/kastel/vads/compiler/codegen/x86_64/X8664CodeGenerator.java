package edu.kit.kastel.vads.compiler.codegen.x86_64;

import java.util.List;
import java.util.Map;

import edu.kit.kastel.vads.compiler.Main;
import edu.kit.kastel.vads.compiler.codegen.CodeGenerator;
import edu.kit.kastel.vads.compiler.codegen.Register;
import edu.kit.kastel.vads.compiler.codegen.RegisterAllocator;
import edu.kit.kastel.vads.compiler.ir.SsaGraph;
import edu.kit.kastel.vads.compiler.ir.nodes.Block;
import edu.kit.kastel.vads.compiler.ir.nodes.BoolNode;
import edu.kit.kastel.vads.compiler.ir.nodes.CallNode;
import edu.kit.kastel.vads.compiler.ir.nodes.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.nodes.Node;
import edu.kit.kastel.vads.compiler.ir.nodes.ParamNode;
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
import edu.kit.kastel.vads.compiler.ir.nodes.binary.NotEqNode;
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

public final class X8664CodeGenerator implements CodeGenerator {

    private final List<SsaGraph> graphs;

    private final StringBuilder builder = new StringBuilder();

    private X8664RegisterAllocator allocator;
    private int nStackRegisters;

    public X8664CodeGenerator(List<SsaGraph> graphs) {
        this.graphs = graphs;
    }

    @Override
    public String generateCode() {
        for (SsaGraph graph : this.graphs) {
            this.allocator = new X8664RegisterAllocator(graph);
            allocator.allocate();
            this.nStackRegisters = allocator.numberOfStackRegisters();
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
        return "$0x" + (value ? 1 : 0);
    }

    private void generateForGraph(SsaGraph graph) {
        if (graph.name().equals("main")) {
            // main needs to be printed as _main
            this.builder.append('_');
        }
        this.builder.append(graph.name()).append(":\n");

        calleeSave();

        if (this.nStackRegisters > 0) {
            moveStackPointer(-this.nStackRegisters * X8664StackRegister.SLOT_SIZE_BYTES);
        }
        
        loadParams(graph.params());

        for (Block block : graph.blocks()) {
            if (block != graph.endBlock()) {
                generateForBlock(block);
            }
        }
    }

    private void generateForBlock(Block block) {
        this.builder.append(block.label())
            .append(":\n");

        for (Node node : block.schedule()) {
            // Generate block-local code
            generateForNode(node, block);
        }
    }

    private void generateForNode(Node node, Block block) {
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
            case NotEqNode notEq -> compare(notEq, "setne");
            case ShiftLeftNode shiftLeft -> shift(shiftLeft, "sall");
            case ShiftRightNode shiftRight -> shift(shiftRight, "sarl");
            case SubNode sub -> defaultBinary(sub, "subl");

            // Control flow nodes
            case ConditionalJumpNode conditionalJump -> conditionalJump(conditionalJump);
            case JumpNode jump -> jump(jump);
            case ReturnNode ret -> ret(ret);

            // Unary operation nodes
            case BitwiseNotNode bitwiseNot -> unary(bitwiseNot, "notl", 8);
            // Logical not is implemented using xor
            case LogicalNotNode logicalNot -> unary(logicalNot, "xorl", 8);
            case NegateNode negate -> unary(negate, "negl", 32);

            // Other nodes
            case BoolNode bool -> constant(fromBoolean(bool.value()), register(bool));
            case CallNode call -> call(call);
            case ConstIntNode constInt -> constant(fromInt(constInt.value()), register(constInt));
            case ParamNode _ -> {
                // Params are handled separately in loadParams()
            }
            case Phi phi -> {
                // Write phis corresponding predecessor value into phi
                Node src = predecessorSkipProj(phi, block.phiIndex(phi));
                move(register(src), register(phi));
            }
            case Block _,ProjNode _,StartNode _ -> {
                // do nothing
            }
            default -> throw new UnsupportedOperationException(
                "code generation for " + node.getClass() + " not yet implemented"
            );
        }
    }

    private void defaultBinary(BinaryOperationNode node, String opcode) {
        Register left = register(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register right = register(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        Register dest = register(node);

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
        Register input = register(predecessorSkipProj(node, UnaryOperationNode.IN));
        Register dest = register(node);

        if (dest instanceof X8664StackRegister) {
            move(input, X8664Register.RAX);
            printUnary(opcode, X8664Register.RAX.name(32));
            move(X8664Register.RAX, dest);
            return;
        }

        move(input, dest);
        printUnary(opcode, dest.name(32));
    }

    private void printUnary(String opcode, String dest) {
        // Logical not is implemented using xor
        if (opcode.equals("xorl")) {
            this.builder.repeat(" ", 2)
                .append(opcode)
                .append(" ")
                .append(fromBoolean(true))
                .append(", ")
                .append(dest)
                .append("\n");
            return;
        }

        this.builder.repeat(" ", 2)
            .append(opcode)
            .append(" ")
            .append(dest)
            .append("\n");
    }

    private void shift(BinaryOperationNode node, String opcode) {
        Register src = register(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register count = register(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        Register dest = register(node);

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
        Register left = register(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register right = register(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        Register dest = register(node);

        if (right instanceof X8664StackRegister) {
            move(right, X8664Register.RAX);
            printCompare(left.name(32), X8664Register.RAX.name(32), dest.name(8), opcode);
            return;
        }

        printCompare(left.name(32), right.name(32), dest.name(8), opcode);
    }

    private void printCompare(String left, String right, String dest, String opcode) {
        // Compare operands are switched in at&t for some reason
        this.builder.repeat(" ", 2)
            .append("cmp ")
            .append(right)
            .append(", ")
            .append(left)
            .append("\n");

        this.builder.repeat(" ", 2)
            .append(opcode)
            .append(" ")
            .append(dest)
            .append("\n");
    }

    private void specialSub(Register src, Register dest) {
        move(src, X8664Register.RAX);
        sourceDest("subl", dest, X8664Register.RAX);
        move(X8664Register.RAX, dest);
    }

    private void division(BinaryOperationNode node) {
        Register left = register(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register right = register(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        Register dest = register(node);

        if (dest == null) {
            // This means that [node]'s result is never actually used. However, [node] is
            // still in the schedule because its side effects are relevant.
            // Just use rax as dest.
            dest = X8664Register.RAX;
        }

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

    private void conditionalJump(ConditionalJumpNode node) {
        Register condition = register(predecessorSkipProj(node, ConditionalJumpNode.CONDITION));
        if (condition instanceof X8664StackRegister) {
            move(condition, X8664Register.RAX);

            // Sets ZF to 0 if condition == false
            this.builder.repeat(" ", 2)
                .append("test ")
                .append(X8664Register.RAX.name(8))
                .append(", ")
                .append(X8664Register.RAX.name(8))
                .append("\n");
        } else {
            // Sets ZF to 0 if condition == false
            this.builder.repeat(" ", 2)
                .append("test ")
                .append(condition.name(8))
                .append(", ")
                .append(condition.name(8)).append("\n");
        }

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
            register(predecessorSkipProj(node, ReturnNode.RESULT)),
            X8664Register.RAX
        );

        if (this.nStackRegisters > 0) {
            moveStackPointer(this.nStackRegisters * X8664StackRegister.SLOT_SIZE_BYTES);
        }

        calleeLoad();

        this.builder.repeat(" ", 2)
            .append("ret")
            .append("\n");
    }

    // Functions

    private void call(CallNode call) {
        callerSave();

        loadArgs(call.args());

        this.builder.repeat(" ", 2)
            .append("call ")
            .append(call.calledFunctioName().asString())
            .append("\n");

        // Load result
        move(X8664Register.RAX, register(call));

        callerLoad();
    }

    private void loadParams(List<ParamNode> params) {
        // TODO this is wonky

        if (params.size() >= 5) {
            // The 4th param (0-indexed) is stored in %r8. Move it beforehand so it doesn't
            // get overwritten.
            Register src4 = paramRegister(4);
            Register dest4 = register(params.get(4));
            if (params.size() >= 6) {
                // The 5th param (0-indexed) is stored in %r8. Move it beforehand so it doesn't
                // get overwritten.
                Register src5 = paramRegister(5);
                Register dest5 = register(params.get(5));

                if (dest4 == X8664Register.R9) {
                    // Need to temporarily store %r8 in %rax
                    move(src4, X8664Register.RAX);
                    move(src5, dest5);
                    move(X8664Register.RAX, dest4);
                } else {
                    move(src4, dest4);
                    move(src5, dest5);
                }
            } else {
                move(src4, dest4);
            }
        }

        for (int id = 0; id < params.size(); id++) {
            if (id == 4 || id == 5) {
                // The 4th and 5th params have already been moved
                continue;
            }

            move(paramRegister(id), register(params.get(id)));
        }
    }

    private void loadArgs(List<Node> args) {
        // TODO this is wonky

        if (args.size() >= 5) {
            Register src4 = register(args.get(4));
            Register dest4 = paramRegister(4);

            if (args.size() >= 6) {
                Register src5 = register(args.get(5));
                Register dest5 = paramRegister(5);

                if (dest4 == X8664Register.R9) {
                    // Need to temporarily store %r8 in %rax
                    move(src4, X8664Register.RAX);
                    move(src5, dest5);
                    move(X8664Register.RAX, dest4);
                } else {
                    move(src4, dest4);
                    move(src5, dest5);
                }
            } else {
                move(src4, dest4);
            }
        }

        for (int id = 0; id < args.size(); id++) {
            if (id == 4 || id == 5) {
                // The 4th and 5th args have already been moved
                continue;
            }

            move(register(args.get(id)), paramRegister(id));
        }
    }

    private void move(Register src, Register dest) {
        if (dest == null) {
            // TODO this is kinda wonky
            // Result isn't used
            return;
        }
        if (src == dest) {
            // Unnecessary move
            return;
        }

        if (src instanceof X8664StackRegister && dest instanceof X8664StackRegister) {
            // Can't move directly between stack slots, need to take detour over rax
            move(src, X8664Register.RAX);
            move(X8664Register.RAX, dest);
            return;
        }

        this.builder.repeat(" ", 2)
            .append("movl ")
            .append(src.name(32))
            .append(", ")
            .append(dest.name(32))
            .append("\n");
    }

    private void constant(String constant, Register dest) {
        this.builder.repeat(" ", 2)
            .append("movl ")
            .append(constant)
            .append(", ")
            .append(dest.name(32))
            .append("\n");
    }

    private void sourceDest(String opcode, Register src, Register dest) {
        this.builder.repeat(" ", 2)
            .append(opcode)
            .append(" ")
            .append(src.name(32))
            .append(", ")
            .append(dest.name(32))
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

    private void calleeSave() {
        this.builder.append("  #callee save\n");
    }

    private void calleeLoad() {
        this.builder.append("  #callee load\n");
    }

    private void callerSave() {
        this.builder.append("  #caller save\n");
    }

    private void callerLoad() {
        // TODO don't load %rax, as the result will be stored in here
        this.builder.append("  #caller load\n");
    }

    // Helper methods

    private Register register(Node node) {
        return this.allocator.register(node);
    }

    private Register paramRegister(int id) {
        return this.allocator.paramRegister(id);
    }
}
