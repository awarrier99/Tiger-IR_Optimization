package optimization;

import ir.IRInstruction;
import java.util.HashSet;
import java.util.ArrayList;

public class BasicBlock {
    public final ArrayList<IRInstruction> instructions;
    public BasicBlock unconditionalSuccessor;
    public BasicBlock falseSuccessor;
    public BasicBlock trueSuccessor;
    public HashSet<Integer> in;
    public String name;
    public ArrayList<BasicBlock> predecessors;
    public HashSet<Integer> out;
    public HashSet<Integer> gen;
    public HashSet<Integer> kill;
    public BasicBlock(ArrayList<IRInstruction> instructions) {
        this.instructions = instructions;
    }
}
