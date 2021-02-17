package optimization;

import ir.IRInstruction;
import java.util.HashSet;
import java.util.ArrayList;

public class BasicBlock {
    public final int name;
    public final ArrayList<IRInstruction> instructions;
    public final ArrayList<BasicBlock> predecessors = new ArrayList<>();
    public final ArrayList<BasicBlock> successors = new ArrayList<>();

    public HashSet<Integer> in;
    public HashSet<Integer> out;
    public HashSet<Integer> gen;
    public HashSet<Integer> kill;

    public BasicBlock(int name, ArrayList<IRInstruction> instructions) {
        this.name = name;
        this.instructions = instructions;
    }
}
