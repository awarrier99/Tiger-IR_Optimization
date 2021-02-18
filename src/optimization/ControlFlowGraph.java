package optimization;

import ir.IRInstruction;

import java.util.HashSet;
import java.util.Set;

public class ControlFlowGraph {
    public final BasicBlock entry;

    public ControlFlowGraph(BasicBlock entry) {
        this.entry = entry;
    }

    private BasicBlock find(BasicBlock curr, IRInstruction instruction, Set<BasicBlock> visited) {
        if (curr == null) return null;
        visited.add(curr);

        for (IRInstruction inst: curr.instructions) {
            if (inst == instruction) return curr;
        }

        BasicBlock found;
        for (BasicBlock block: curr.successors) {
            if (!visited.contains(block)) {
                found = this.find(block, instruction, visited);
                if (found != null) return found;
            }
        }

        return null;
    }

    public BasicBlock find(BasicBlock curr, IRInstruction instruction) {
        return this.find(curr, instruction, new HashSet<>());
    }
}
