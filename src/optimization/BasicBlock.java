package optimization;

import ir.IRInstruction;

import java.util.ArrayList;

public class BasicBlock {
    public final ArrayList<IRInstruction> instructions;
    public BasicBlock unconditionalSuccessor;
    public BasicBlock falseSuccessor;
    public BasicBlock trueSuccessor;

    public BasicBlock(ArrayList<IRInstruction> instructions) {
        this.instructions = instructions;
    }
}
