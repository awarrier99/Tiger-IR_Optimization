package optimization;

import ir.IRInstruction;

import java.util.ArrayList;

public class BasicBlock {
    public final int name;
    public final ArrayList<IRInstruction> instructions;
    public final ArrayList<BasicBlock> predecessors = new ArrayList<>();
    public final ArrayList<BasicBlock> successors = new ArrayList<>();
    public BasicBlock unconditionalSuccessor;
    public BasicBlock falseSuccessor;
    public BasicBlock trueSuccessor;

    public BasicBlock(int name, ArrayList<IRInstruction> instructions) {
        this.name = name;
        this.instructions = instructions;
    }
}
