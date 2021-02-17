package optimization;

import ir.*;
import ir.operand.IRLabelOperand;

import java.io.FileNotFoundException;
import java.util.*;

public class Optimizer {
    private static final Set<IRInstruction.OpCode> branchCodes = new HashSet<>();

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

    public static void main(String[] args) throws Exception {
        Optimizer optimizer = new Optimizer(args[0]);
        ControlFlowGraph graph = optimizer.buildControlFlowGraph();
        Debug.printControlFlowGraph(graph);
    }
}
