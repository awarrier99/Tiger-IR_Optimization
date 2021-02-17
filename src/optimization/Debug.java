package optimization;

import ir.IRInstruction;
import ir.operand.IROperand;

import java.util.HashMap;
import java.util.Map;

public class Debug {
    private static Map<BasicBlock, Integer> blockMap;

    private static void printInstruction(IRInstruction instruction, String prefix) {
        System.out.print(prefix + "Instruction: ");
        System.out.print(instruction.opCode.toString() + ", ");
        for (int i = 0; i < instruction.operands.length; i++) {
            IROperand operand = instruction.operands[i];
            System.out.print(operand.toString());
            if (i != instruction.operands.length - 1) System.out.print(", ");
        }
        System.out.println();
    }

    private static void printBasicBlock(BasicBlock block, String prefix, int num) {
        if (block == null) return;

        blockMap.put(block, num);
        System.out.println(prefix + "Block:");
        prefix += "\t";
        System.out.println(prefix + "Name: " + num);

        System.out.println(prefix + "Instructions:");
        for (IRInstruction instruction: block.instructions) {
            printInstruction(instruction, prefix + "\t");
        }

        System.out.println(prefix + "Unconditional: ");
        if (blockMap.containsKey(block.unconditionalSuccessor)) System.out.println(prefix + blockMap.get(block.unconditionalSuccessor));
        else printBasicBlock(block.unconditionalSuccessor, prefix + "\t", num + 1);
    }

    public static void printControlFlowGraph(ControlFlowGraph graph) {
        blockMap = new HashMap<>();
        printBasicBlock(graph.entry, "", 0);
    }
}
