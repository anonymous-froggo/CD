package edu.kit.kastel.vads.compiler.codegen.x86_64;

import java.util.Arrays;
import java.util.Collections;
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
            generateForGraph(graph);
        }
        
        libFunctions();

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
        // Mangle function names
        this.builder.append('_')
            .append(graph.name())
            .append(":\n");
        calleeSave();
        X8664StackRegister.resetCurrentStackPointerOffset();

        this.allocator = new X8664RegisterAllocator(graph);
        this.allocator.allocate();
        this.nStackRegisters = this.allocator.numberOfStackRegisters();
        if (Main.DEBUG) {
            this.allocator.printAllocation();
        }
        if (this.nStackRegisters > 0) {
            // Allocate space for stack registers
            moveStackPointer(-this.nStackRegisters * X8664StackRegister.SLOT_SIZE);
            // This is the baseline stack pointer offset for the starting block, so reset
            X8664StackRegister.resetCurrentStackPointerOffset();
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
            moveStackPointer(this.nStackRegisters * X8664StackRegister.SLOT_SIZE);
        }

        calleeRestore();

        this.builder.repeat(" ", 2)
            .append("ret")
            .append("\n");

        X8664StackRegister.resetCurrentStackPointerOffset();
    }

    // Functions

    private void call(CallNode call) {
        callerSave();

        loadArgs(call.args());

        this.builder.repeat(" ", 2)
            // Mangle function names
            .append("call _")
            .append(call.calledFunctioName().asString())
            .append("\n");

        unloadArgs(call.args());

        callerRestore();

        // Load result
        move(X8664Register.RAX, register(call));
    }

    private void loadParams(List<ParamNode> params) {
        for (int id = 0; id < params.size(); id++) {
            move(paramRegister(id), register(params.get(id)));
        }
    }

    private void loadArgs(List<Node> args) {
        for (int id = args.size() - 1; id >= 0; id--) {
            push(register(args.get(id)));
        }
    }

    private void unloadArgs(List<Node> args) {
        moveStackPointer(X8664StackRegister.SLOT_SIZE * args.size());
    }

    // TODO eliminate unnecessary saves/loads

    private void calleeSave() {
        for (Register register : X8664Register.calleeSavedRegisters()) {
            push(register);
        }
    }

    private void calleeRestore() {
        // Need to pop in reverse order
        Register[] calleeSavedRegisters = X8664Register.calleeSavedRegisters();
        for (int i = calleeSavedRegisters.length - 1; i >= 0; i--) {
            pop(calleeSavedRegisters[i]);
        }
    }

    private void callerSave() {
        for (Register register : X8664Register.callerSavedRegisters()) {
            push(register);
        }
    }

    private void callerRestore() {
        // Need to pop in reverse order
        Register[] callerSavedRegisters = X8664Register.callerSavedRegisters();
        for (int i = callerSavedRegisters.length - 1; i >= 0; i--) {
            pop(callerSavedRegisters[i]);
        }
    }

    private void move(Register src, Register dest) {
        if (dest == null) {
            // TODO this is wonky
            // Unused result
            if (Main.DEBUG) {
                System.out.println("Unused dest: " + dest);
            }
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

    private void push(Register src) {
        this.builder.repeat(" ", 2)
            .append("pushq ")
            .append(src.name(64))
            .append("\n");

        X8664StackRegister.moveStackPointer(-X8664StackRegister.SLOT_SIZE);
    }

    private void pop(Register dest) {
        this.builder.repeat(" ", 2)
            .append("popq ")
            .append(dest.name(64))
            .append("\n");

        X8664StackRegister.moveStackPointer(X8664StackRegister.SLOT_SIZE);
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

        X8664StackRegister.moveStackPointer(offset);
    }

    private void libFunctions() {
        this.builder.append("_print:\n");
        calleeSave();
        X8664StackRegister.resetCurrentStackPointerOffset();
        move(paramRegister(0), X8664Register.RDI);
        this.builder.append("  call putchar\n");
        calleeRestore();

        this.builder.append("_flush:\n");
        calleeSave();
        X8664StackRegister.resetCurrentStackPointerOffset();
        this.builder.append("  call get_stdout\n");
        move(X8664Register.RDI, X8664Register.RAX);
        this.builder.append("  call fflush\n");
        calleeRestore();
    }

    // Helper methods

    private Register register(Node node) {
        return this.allocator.register(node);
    }

    private Register paramRegister(int id) {
        return this.allocator.paramRegister(id);
    }
}
