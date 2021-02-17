package optimization;

import ir.*;
import ir.operand.IRLabelOperand;
import ir.operand.IROperand;
import java.io.FileNotFoundException;
import java.util.*;

public class Optimizer {
    private static final Set<IRInstruction.OpCode> branchCodes = new HashSet<>();
    private static final Set<IRInstruction.OpCode> defCodes = new HashSet<>();
    private final IRProgram program;
    private ControlFlowGraph controlFlowGraph;

    public Optimizer(String filename) throws FileNotFoundException, IRException {
        IRReader irReader = new IRReader();
        this.program = irReader.parseIRFile(filename);
//        initProgram();

        branchCodes.add(IRInstruction.OpCode.BREQ);
        branchCodes.add(IRInstruction.OpCode.BRGEQ);
        branchCodes.add(IRInstruction.OpCode.BRGT);
        branchCodes.add(IRInstruction.OpCode.BRLT);
        branchCodes.add(IRInstruction.OpCode.BRLEQ);
        branchCodes.add(IRInstruction.OpCode.BRNEQ);

        defCodes.add(IRInstruction.OpCode.ASSIGN);
        defCodes.add(IRInstruction.OpCode.ADD);
        defCodes.add(IRInstruction.OpCode.SUB);
        defCodes.add(IRInstruction.OpCode.MULT);
        defCodes.add(IRInstruction.OpCode.DIV);
        defCodes.add(IRInstruction.OpCode.AND);
        defCodes.add(IRInstruction.OpCode.OR);

    }

    private ControlFlowGraph buildControlFlowGraph() {
        BasicBlock block = new BasicBlock(new ArrayList<>());
        ControlFlowGraph graph = new ControlFlowGraph(block);
        Map<IRFunction, BasicBlock> functionBlockMap = new HashMap<>();

        for (IRFunction function: this.program.functions) {
            Map<IRInstruction, BasicBlock> instructionBlockMap = new HashMap<>();
            Map<String, IRInstruction> labelMap = new HashMap<>();

            for (int i = 0; i < function.instructions.size(); i++) {
                IRInstruction instruction = function.instructions.get(i);

                if (instruction.opCode == IRInstruction.OpCode.LABEL) {
                    labelMap.put(((IRLabelOperand) instruction.operands[0]).getName(), instruction);
                    ArrayList<IRInstruction> instructions = new ArrayList<>();
                    instructions.add(instruction);
                    instructionBlockMap.put(instruction, new BasicBlock(instructions));
                }
            }

            functionBlockMap.put(function, block);

            for (int i = 0; i < function.instructions.size(); i++) {
                IRInstruction instruction = function.instructions.get(i);

                if (instruction.opCode == IRInstruction.OpCode.LABEL) {
                    String label = ((IRLabelOperand) instruction.operands[0]).getName();
                    block = instructionBlockMap.get(labelMap.get(label));
                } else if (instruction.opCode == IRInstruction.OpCode.GOTO) {
                    block.instructions.add(instruction);
                    String label = ((IRLabelOperand) instruction.operands[0]).getName();
                    block.unconditionalSuccessor = instructionBlockMap.get(labelMap.get(label));
                } else if (branchCodes.contains(instruction.opCode)) {
                    String label = ((IRLabelOperand) instruction.operands[0]).getName();
                    block.trueSuccessor = instructionBlockMap.get(labelMap.get(label));
                } else {
                    block.instructions.add(instruction);
                }
            }
        }

        return graph;
    }

    private void generateReachDefinitions(ControlFlowGraph cfg)
    {
        Map<IROperand, ArrayList<Integer>> definitions = new HashMap<>();
        Set<String> reachedBlocks = new HashSet<>();
        BasicBlock head = cfg.entry;
        reachDefinitionsHelper(head, reachedBlocks, definitions);

    }

    private void reachDefinitionsHelper(BasicBlock head, Set<String> reachedBlocks, Map<IROperand, ArrayList<Integer>> definitions)
    {
        IROperand operand;
        ArrayList<Integer> defs;
        reachedBlocks.add(head.name);
        IRInstruction curInst;
        for (int i = 0; i < head.instructions.size(); i++)
        {
            curInst = head.instructions.get(i);
            if (defCodes.contains(curInst)) {
                head.out.add(curInst.irLineNumber);
                head.gen.add(curInst.irLineNumber);
                operand = curInst.operands[0];
                if (!definitions.containsKey(operand)) {
                    definitions.put(operand, new ArrayList<Integer>());
                }
                definitions.get(operand).add(curInst.irLineNumber);
            }
        }

        if (head.unconditionalSuccessor != null && !reachedBlocks.contains(head.unconditionalSuccessor.name))
        {
            reachDefinitionsHelper(head.unconditionalSuccessor, reachedBlocks, definitions);
        }
        if (head.falseSuccessor != null && !reachedBlocks.contains(head.falseSuccessor.name))
        {
            reachDefinitionsHelper(head.falseSuccessor, reachedBlocks, definitions);
        }
        if (head.trueSuccessor != null && !reachedBlocks.contains(head.trueSuccessor.name))
        {
            reachDefinitionsHelper(head.trueSuccessor, reachedBlocks, definitions);
        }

        for (int i = 0; i < head.instructions.size(); i++) {
            if (defCodes.contains(head.instructions.get(i))) {
                defs = definitions.get(head.instructions.get(i).operands[0]);
                head.kill.addAll(defs);
            }
        }
        head.kill.removeAll(head.gen);
    }

    public static void main(String[] args) throws Exception {
        Optimizer optimizer = new Optimizer(args[0]);
        ControlFlowGraph graph = optimizer.buildControlFlowGraph();
        Debug.printControlFlowGraph(graph);
    }
}
