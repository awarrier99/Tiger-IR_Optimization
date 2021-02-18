package optimization;

import ir.*;
import ir.operand.IRFunctionOperand;
import ir.operand.IRLabelOperand;
import ir.operand.IROperand;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;

public class Optimizer {
    private static final Set<IRInstruction.OpCode> branchCodes = new HashSet<>();
    private static final Set<String> intrinsicFunctions = new HashSet<>();
    private static int num = 0;
    private static final Set<IRInstruction.OpCode> defCodes = new HashSet<>();
    private static final Set<IRInstruction.OpCode> criticalCodes = new HashSet<>();

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

        defCodes.add(IRInstruction.OpCode.ASSIGN);
        defCodes.add(IRInstruction.OpCode.ADD);
        defCodes.add(IRInstruction.OpCode.SUB);
        defCodes.add(IRInstruction.OpCode.MULT);
        defCodes.add(IRInstruction.OpCode.DIV);
        defCodes.add(IRInstruction.OpCode.AND);
        defCodes.add(IRInstruction.OpCode.OR);
        defCodes.add(IRInstruction.OpCode.ARRAY_LOAD);
        defCodes.add(IRInstruction.OpCode.CALLR);

        criticalCodes.add(IRInstruction.OpCode.CALL);
        criticalCodes.add(IRInstruction.OpCode.CALLR);
        criticalCodes.add(IRInstruction.OpCode.ARRAY_STORE);
        criticalCodes.add(IRInstruction.OpCode.RETURN);
        criticalCodes.add(IRInstruction.OpCode.GOTO);
        criticalCodes.addAll(branchCodes);
        criticalCodes.add(IRInstruction.OpCode.LABEL);
    }

    private Set<IRInstruction> getLeaders(Map<IRFunction, Map<String, IRInstruction>> labelMap) {
        boolean branchSuccessor = false;
        boolean funcSuccessor = false;
        Set<IRInstruction> leaders = new HashSet<>();
        for (IRFunction function: this.program.functions) {
            labelMap.put(function, new HashMap<>());
            for (int i = 0; i < function.instructions.size(); i++) {
                IRInstruction instruction = function.instructions.get(i);
                if (instruction.opCode == IRInstruction.OpCode.LABEL) {
                    leaders.add(instruction);
                    labelMap.get(function).put(((IRLabelOperand) instruction.operands[0]).getName(), instruction);
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

    private ControlFlowGraph buildControlFlowGraph(Map<IRInstruction, BasicBlock> blockMap) {
        Map<IRFunction, Map<String, IRInstruction>> labelMap = new HashMap<>();
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
                    blockMap.put(instruction, curr);
                    if (i == 0) functionBlockMap.put(function.name, curr);
                }
                if (instruction.opCode == IRInstruction.OpCode.GOTO) {
                    if (!isLeader) curr.instructions.add(instruction);
                    String label = ((IRLabelOperand) instruction.operands[0]).getName();
                    BasicBlock block = leaderBlockMap.get(labelMap.get(function).get(label));
                    curr.successors.add(block);
                    block.predecessors.add(curr);
                    blockMap.put(instruction, curr);
                    curr = null;
                } else if (branchCodes.contains(instruction.opCode)) {
                    if (!isLeader) curr.instructions.add(instruction);
                    String label = ((IRLabelOperand) instruction.operands[0]).getName();
                    BasicBlock block = leaderBlockMap.get(labelMap.get(function).get(label));
                    curr.successors.add(block);
                    block.predecessors.add(curr);
                    block = leaderBlockMap.get(function.instructions.get(i + 1));
                    curr.successors.add(block);
                    block.predecessors.add(curr);
                    blockMap.put(instruction, curr);
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
                        blockMap.put(instruction, curr);
                        curr = null;
                    }
                } else if (instruction.opCode == IRInstruction.OpCode.RETURN || i == function.instructions.size() - 1) {
                    if (!isLeader) curr.instructions.add(instruction);
                    returnBlockMap.put(function.name, curr);
                    while (!returnCache.isEmpty()) {
                        BasicBlock block = leaderBlockMap.get(returnCache.remove(0));
                        curr.successors.add(block);
                        block.predecessors.add(curr);
                    }
                    blockMap.put(instruction, curr);
                    curr = null;
                } else if (!isLeader) {
                    curr.instructions.add(instruction);
                    blockMap.put(instruction, curr);
                }
            }
        }

        return new ControlFlowGraph(entry);
    }

    private Map<Integer, IRInstruction> generateReachDefinitions(ControlFlowGraph cfg)
    {
        Map<String, ArrayList<Integer>> definitions = new HashMap<>();
        Set<Integer> reachedBlocks = new HashSet<>();
        Map<Integer, IRInstruction> lineToInst = new HashMap<>();
        Map<Integer, HashSet<Integer>> outSet = new HashMap<>();
        Map<Integer, HashSet<Integer>> tempOutSet = new HashMap<>();
        BasicBlock head = cfg.entry;
        reachDefinitionsHelper(head, reachedBlocks, definitions, outSet, lineToInst);
        reachedBlocks.clear();

        while(!outSet.equals(tempOutSet))
        {
            tempOutSet = outSet;
            reachDefinitionsHelper2(head, tempOutSet, reachedBlocks);
        }

        return lineToInst;

    }

    private void reachDefinitionsHelper2 (BasicBlock head, Map<Integer, HashSet<Integer>> outSet, Set<Integer> reachedBlocks)
    {
        reachedBlocks.add(head.name);
        head.predecessors.forEach((pred) -> head.in.addAll(pred.out));
        head.out = new HashSet<>(head.gen);
        HashSet<Integer> added = new HashSet<>(head.in);
        added.removeAll(head.kill);
        head.out.addAll(added);
        if (outSet.containsKey(head.name)) {
            outSet.replace(head.name, head.out);
        } else {
            outSet.put(head.name, head.out);
        }

        for (int x = 0; x < head.successors.size(); x++) {
            if (!reachedBlocks.contains(head.successors.get(x).name)) {
                reachDefinitionsHelper2(head.successors.get(x), outSet, reachedBlocks);
            }
        }
    }

    private void reachDefinitionsHelper
            (BasicBlock head,
             Set<Integer> reachedBlocks,
             Map<String, ArrayList<Integer>> definitions,
             Map<Integer, HashSet<Integer>> outSet,
             Map<Integer, IRInstruction> lineToInst)
    {
        IROperand operand;
        ArrayList<Integer> defs;
        reachedBlocks.add(head.name);
        IRInstruction curInst;
        for (int i = 0; i < head.instructions.size(); i++)
        {
            curInst = head.instructions.get(i);
            if (defCodes.contains(curInst.opCode)) {
                lineToInst.put(curInst.irLineNumber, curInst);
                head.out.add(curInst.irLineNumber);
                head.gen.add(curInst.irLineNumber);
                operand = curInst.operands[0];
                if (!definitions.containsKey(operand.toString())) {
                    definitions.put(operand.toString(), new ArrayList<Integer>());
                }
                definitions.get(operand.toString()).add(curInst.irLineNumber);
            }
        }
        outSet.put(head.name, head.out);
        for (int x = 0; x < head.successors.size(); x++) {
            if (!reachedBlocks.contains(head.successors.get(x).name)) {
                reachDefinitionsHelper(head.successors.get(x), reachedBlocks, definitions, outSet, lineToInst);
            }
        }

        for (int i = 0; i < head.instructions.size(); i++) {
            if (defCodes.contains(head.instructions.get(i).opCode)) {
                defs = definitions.get(head.instructions.get(i).operands[0].toString());
                head.kill.addAll(defs);
            }
        }
        if (!head.gen.isEmpty()) {
            head.kill.removeAll(head.gen);
        }
    }

    private void optimize() {
        Map<IRInstruction, BasicBlock> blockMap = new HashMap<>();
        ControlFlowGraph graph = this.buildControlFlowGraph(blockMap);
        Map<Integer, IRInstruction> linetoInst = this.generateReachDefinitions(graph);
        Debug.printControlFlowGraph(graph);

        Set<IRInstruction> critical = new HashSet<>();
        ArrayList<IRInstruction> worklist = new ArrayList<>();
        for (IRFunction function: this.program.functions) {
            for (IRInstruction instruction: function.instructions) {
                if (criticalCodes.contains(instruction.opCode)) {
                    critical.add(instruction);
                    worklist.add(instruction);
//                    Debug.printInstruction(instruction, "");
                }
            }
        }

        while (!worklist.isEmpty()) {
            IRInstruction instruction = worklist.remove(0);
            if (instruction.opCode == IRInstruction.OpCode.LABEL) continue;
            BasicBlock block = graph.find(graph.entry, instruction);
            Debug.printInstruction(instruction, "");

            Map<String, IRInstruction> latestDefs = new HashMap<>();
            for (int i = 0; i < block.instructions.size(); i++) {
                IRInstruction def = block.instructions.get(i);
                if (def == instruction) break;
                if (!defCodes.contains(def.opCode)) continue;

                for (int j = 0; j < instruction.operands.length; j++) {
                    if (j == 0 && instruction.opCode != IRInstruction.OpCode.RETURN) continue;

                    IROperand operand = instruction.operands[j];
                    if (operand.toString().equals(def.operands[0].toString())) {
                        latestDefs.put(operand.toString(), def);
                    }
                }
            }

            if (!latestDefs.isEmpty()) {
                System.out.println("Local defs:");
                for (String op: latestDefs.keySet()) {
                    IRInstruction latestDef = latestDefs.get(op);
                    Debug.printInstruction(latestDef, "\t");
                    if (!critical.contains(latestDef)) {
                        critical.add(latestDef);
                        worklist.add(latestDef);
                    }
                }
            }

            System.out.println("Defs:");
            for (int line: block.in) {
                IRInstruction def = linetoInst.get(line);
                if (latestDefs.containsKey(def.operands[0].toString())) continue;
                Debug.printInstruction(def, "\t");
                for (int i = 0; i < instruction.operands.length; i++) {
                    if (i == 0 && instruction.opCode != IRInstruction.OpCode.RETURN) continue;

                    IROperand operand = instruction.operands[i];
                    if (operand.toString().equals(def.operands[0].toString())) {
                        if (!critical.contains(def)) {
                            critical.add(def);
                            worklist.add(def);
                            break;
                        }
                    }
                }
            }

            System.out.println();
        }

        for (IRFunction function: this.program.functions) {
            ArrayList<IRInstruction> instructions = new ArrayList<>();
            for (IRInstruction instruction: function.instructions) {
                if (critical.contains(instruction)) instructions.add(instruction);
            }
            function.instructions = instructions;
        }
    }

    public static void main(String[] args) throws Exception {
        String filename = args[0];
        Optimizer optimizer = new Optimizer(filename);
        optimizer.optimize();
//        System.out.println("\nOptimized:");
//        for (IRInstruction instruction: optimized) Debug.printInstruction(instruction, "");

        IRPrinter printer = new IRPrinter(new PrintStream("optimized/" + filename));
        printer.printProgram(optimizer.program);
    }
}
