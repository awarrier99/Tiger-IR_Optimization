package optimization;

import ir.*;
import ir.operand.IRFunctionOperand;
import ir.operand.IRLabelOperand;

import java.io.FileNotFoundException;
import java.util.*;

public class Optimizer {
    private static final Set<IRInstruction.OpCode> branchCodes = new HashSet<>();
    private static final Set<String> intrinsicFunctions = new HashSet<>();
    private static int num = 0;

    private final IRProgram program;

    public Optimizer(String filename) throws FileNotFoundException, IRException {
        IRReader irReader = new IRReader();
        this.program = irReader.parseIRFile(filename);
        branchCodes.add(IRInstruction.OpCode.BREQ);
        branchCodes.add(IRInstruction.OpCode.BRGEQ);
        branchCodes.add(IRInstruction.OpCode.BRGT);
        branchCodes.add(IRInstruction.OpCode.BRLT);
        branchCodes.add(IRInstruction.OpCode.BRLEQ);
        branchCodes.add(IRInstruction.OpCode.BRNEQ);
        intrinsicFunctions.add("geti");
        intrinsicFunctions.add("puti");
        intrinsicFunctions.add("getc");
        intrinsicFunctions.add("putc");
        intrinsicFunctions.add("getf");
        intrinsicFunctions.add("putf");
    }

//    private ControlFlowGraph buildControlFlowGraph() {
//        BasicBlock block = new BasicBlock(num++, new ArrayList<>());
//        ControlFlowGraph graph = new ControlFlowGraph(block);
//        Map<IRFunction, BasicBlock> functionBlockMap = new HashMap<>();
//
//        for (IRFunction function: this.program.functions) {
//            Map<IRInstruction, BasicBlock> instructionBlockMap = new HashMap<>();
//            Map<String, IRInstruction> labelMap = new HashMap<>();
//
//            for (int i = 0; i < function.instructions.size(); i++) {
//                IRInstruction instruction = function.instructions.get(i);
//
//                if (instruction.opCode == IRInstruction.OpCode.LABEL) {
//                    labelMap.put(((IRLabelOperand) instruction.operands[0]).getName(), instruction);
//                    ArrayList<IRInstruction> instructions = new ArrayList<>();
//                    instructions.add(instruction);
//                    instructionBlockMap.put(instruction, new BasicBlock(num++, instructions));
//                }
//            }
//
//            functionBlockMap.put(function, block);
//
//            for (int i = 0; i < function.instructions.size(); i++) {
//                IRInstruction instruction = function.instructions.get(i);
//
//                if (instruction.opCode == IRInstruction.OpCode.LABEL) {
//                    if (block.instructions.isEmpty()) instructionBlockMap.put(instruction, block);
//                    else block = instructionBlockMap.get(instruction);
//                } else if (instruction.opCode == IRInstruction.OpCode.GOTO) {
//                    block.instructions.add(instruction);
//                    String label = ((IRLabelOperand) instruction.operands[0]).getName();
//                    block.unconditionalSuccessor = instructionBlockMap.get(labelMap.get(label));
//                } else if (branchCodes.contains(instruction.opCode)) {
//                    block.instructions.add(instruction);
//                    String label = ((IRLabelOperand) instruction.operands[0]).getName();
//                    block.trueSuccessor = instructionBlockMap.get(labelMap.get(label));
//                    block.falseSuccessor = new BasicBlock(num++, new ArrayList<>());
//                    block = block.falseSuccessor;
//                } else {
//                    block.instructions.add(instruction);
//                }
//            }
//        }
//
//        return graph;
//    }

    private Set<IRInstruction> getLeaders(Map<String, IRInstruction> labelMap) {
        boolean branchSuccessor = false;
        boolean funcSuccessor = false;
        Set<IRInstruction> leaders = new HashSet<>();
        for (IRFunction function: this.program.functions) {
            for (int i = 0; i < function.instructions.size(); i++) {
                IRInstruction instruction = function.instructions.get(i);
                if (instruction.opCode == IRInstruction.OpCode.LABEL) {
                    leaders.add(instruction);
                    labelMap.put(((IRLabelOperand) instruction.operands[0]).getName(), instruction);
                }
                else if (i == 0) leaders.add(instruction);
                else if (branchSuccessor || funcSuccessor) leaders.add(instruction);

                if (branchSuccessor) branchSuccessor = false;
                if (funcSuccessor) funcSuccessor = false;
                if (branchCodes.contains(instruction.opCode)) branchSuccessor = true;
                if (instruction.opCode == IRInstruction.OpCode.CALL || instruction.opCode == IRInstruction.OpCode.CALLR) {
                    int pos = instruction.opCode == IRInstruction.OpCode.CALL ? 0 : 1;
                    if (!intrinsicFunctions.contains(((IRFunctionOperand) instruction.operands[pos]).getName())) funcSuccessor = true;
                }
            }
        }

        return leaders;
    }

    private ControlFlowGraph buildControlFlowGraph() {
        Map<String, IRInstruction> labelMap = new HashMap<>();
        Map<String, BasicBlock> functionBlockMap = new HashMap<>();
        Map<String, BasicBlock> returnBlockMap = new HashMap<>();
        ArrayList<IRInstruction> returnCache = new ArrayList<>();
        Set<IRInstruction> leaders = this.getLeaders(labelMap);
        Map<IRInstruction, BasicBlock> leaderBlockMap = new HashMap<>();
        for (IRInstruction leader: leaders) {
            ArrayList<IRInstruction> instructions = new ArrayList<>();
            instructions.add(leader);
            leaderBlockMap.put(leader, new BasicBlock(num++, instructions));
        }

        BasicBlock curr = null;
        BasicBlock entry = null;
        for (IRFunction function: this.program.functions) {
            for (int i = 0; i < function.instructions.size(); i++) {
                IRInstruction instruction = function.instructions.get(i);
                Debug.printInstruction(instruction, "");
                if (i == 0 && function.name.equals("main")) entry = leaderBlockMap.get(instruction);
                boolean isLeader = false;
                if (leaders.contains(instruction)) {
                    BasicBlock block = leaderBlockMap.get(instruction);
                    isLeader = true;
                    if (curr != null) {
                        curr.successors.add(block);
                        block.predecessors.add(curr);
                    }
                    curr = block;
                    if (i == 0) functionBlockMap.put(function.name, curr);
                }
                if (instruction.opCode == IRInstruction.OpCode.GOTO) {
                    if (!isLeader) curr.instructions.add(instruction);
                    String label = ((IRLabelOperand) instruction.operands[0]).getName();
                    BasicBlock block = leaderBlockMap.get(labelMap.get(label));
                    curr.successors.add(block);
                    block.predecessors.add(curr);
                    curr = null;
                } else if (branchCodes.contains(instruction.opCode)) {
                    if (!isLeader) curr.instructions.add(instruction);
                    String label = ((IRLabelOperand) instruction.operands[0]).getName();
                    BasicBlock block = leaderBlockMap.get(labelMap.get(label));
                    curr.successors.add(block);
                    block.predecessors.add(curr);
                    block = leaderBlockMap.get(function.instructions.get(i + 1));
                    curr.successors.add(block);
                    block.predecessors.add(curr);
                    curr = null;
                } else if (instruction.opCode == IRInstruction.OpCode.CALL || instruction.opCode == IRInstruction.OpCode.CALLR) {
                    if (!isLeader) curr.instructions.add(instruction);
                    int pos = instruction.opCode == IRInstruction.OpCode.CALL ? 0 : 1;
                    String name = ((IRFunctionOperand) instruction.operands[pos]).getName();
                    if (!intrinsicFunctions.contains(name)) {
                        BasicBlock block = functionBlockMap.get(name);
                        curr.successors.add(block);
                        block.predecessors.add(curr);
                        if (returnBlockMap.containsKey(name)) {
                            block = leaderBlockMap.get(function.instructions.get(i + 1));
                            returnBlockMap.get(name).successors.add(block);
                            block.predecessors.add(returnBlockMap.get(name));
                        }
                        else returnCache.add(function.instructions.get(i + 1));
                        curr = null;
                    }
                } else if (instruction.opCode == IRInstruction.OpCode.RETURN) {
                    if (!isLeader) curr.instructions.add(instruction);
                    returnBlockMap.put(function.name, curr);
                    while (!returnCache.isEmpty()) {
                        BasicBlock block = leaderBlockMap.get(returnCache.remove(0));
                        curr.successors.add(block);
                        block.predecessors.add(curr);
                    }
                    curr = null;
                } else if (!isLeader) curr.instructions.add(instruction);
            }
        }

        return new ControlFlowGraph(entry);
    }

    public static void main(String[] args) throws Exception {
        Optimizer optimizer = new Optimizer(args[0]);
        ControlFlowGraph graph = optimizer.buildControlFlowGraph();
        Debug.printControlFlowGraph(graph);
    }
}
