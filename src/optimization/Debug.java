package optimization;

import ir.IRInstruction;
import ir.operand.IROperand;

import java.util.HashSet;
import java.util.Set;

public class Debug {
    private static Set<BasicBlock> blockSet;

    public static void printInstruction(IRInstruction instruction, String prefix) {
        System.out.print(prefix + "Instruction: ");
        System.out.print(instruction.opCode.toString() + ", ");
        for (int i = 0; i < instruction.operands.length; i++) {
            IROperand operand = instruction.operands[i];
            System.out.print(operand.toString());
            if (i != instruction.operands.length - 1) System.out.print(", ");
        }
        System.out.println();
    }

    private static void printBasicBlock(BasicBlock block, String prefix) {
        if (block == null) return;

        blockSet.add(block);
        System.out.println(prefix + "Block:");
        prefix += "\t";
        System.out.println(prefix + "Name: " + block.name);

        System.out.println(prefix + "Instructions:");
        for (IRInstruction instruction: block.instructions) {
            printInstruction(instruction, prefix + "\t");
        }

        System.out.println(prefix + "In:");
        for (int in: block.in) {
            System.out.println(prefix + "\t" + in + "\t");
        }

        System.out.println(prefix + "Out:");
        for (int out: block.out) {
            System.out.println(prefix + "\t" + out + "\t");
        }

        System.out.println(prefix + "Gen:");
        for (int gen: block.gen) {
            System.out.println(prefix + "\t" + gen + "\t");
        }

        System.out.println(prefix + "Kill:");
        for (int kill: block.kill) {
            System.out.println(prefix + "\t" + kill + "\t");
        }
        for (BasicBlock predecessor: block.predecessors) {
            System.out.println(prefix + "Predecessor: " + predecessor.name);
        }

        for (BasicBlock successor: block.successors) {
            System.out.println(prefix + "Successor: ");
            if (blockSet.contains(successor))
                System.out.println(prefix + successor.name);
            else printBasicBlock(successor, prefix + "\t");
        }

//        if (block.unconditionalSuccessor != null) {
//            System.out.println(prefix + "Unconditional: ");
//            if (blockSet.contains(block.unconditionalSuccessor))
//                System.out.println(prefix + block.unconditionalSuccessor.name);
//            else printBasicBlock(block.unconditionalSuccessor, prefix + "\t");
//        }
//
//        if (block.trueSuccessor != null) {
//            System.out.println(prefix + "True: ");
//            if (blockSet.contains(block.trueSuccessor))
//                System.out.println(prefix + block.trueSuccessor.name);
//            else printBasicBlock(block.trueSuccessor, prefix + "\t");
//        }
//
//        if (block.falseSuccessor != null) {
//            System.out.println(prefix + "False: ");
//            if (blockSet.contains(block.falseSuccessor))
//                System.out.println(prefix + block.falseSuccessor.name);
//            else printBasicBlock(block.falseSuccessor, prefix + "\t");
//        }
    }

    public static void printControlFlowGraph(ControlFlowGraph graph) {
        blockSet = new HashSet<>();
        printBasicBlock(graph.entry, "");
    }
}
