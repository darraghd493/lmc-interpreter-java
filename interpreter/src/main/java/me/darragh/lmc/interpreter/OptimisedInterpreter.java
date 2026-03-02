package me.darragh.lmc.interpreter;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import me.darragh.lmc.Instruction;
import me.darragh.lmc.Opcode;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * An optimised interpreter for LMC. It does not faithfully follow the fetch-decode-execute cycle,
 * instead it simply iterates through the instructions and executes them directly.
 *
 * @author darraghd493
 * @since 1.0.0
 */
@Slf4j
@Builder
public class OptimisedInterpreter implements Interpreter {
    private final @NotNull Instruction[] instructions;
    private final @NotNull IoHandler ioHandler;

    private final Map<Integer, Integer> memory = new HashMap<>();

    private int programCounter, accumulator;

    @Override
    public void prepare() {
        for (int i = 0; i < this.instructions.length; i++) {
            Instruction instruction = this.instructions[i];
            if (instruction.opcode() == Opcode.DAT) {
                this.memory.put(i, instruction.operand());
            }
        }
    }

    @Override
    public boolean step() throws InterpreterException {
        if (this.programCounter >= this.instructions.length) {
            throw new RuntimeException("Program counter out of bounds");
        }

        Instruction instruction = this.instructions[this.programCounter]; // fetch
        this.programCounter++;

        return this.execute(instruction);
    }

    private boolean execute(@NotNull Instruction instruction) throws InterpreterException {
        boolean result = true;
        switch (instruction.opcode()) {
            case HLT -> result = false;
            case ADD -> this.accumulator += this.memory.getOrDefault(instruction.operand(), 0);
            case SUB -> this.accumulator -= this.memory.getOrDefault(instruction.operand(), 0);
            case STA, STO -> this.memory.put(instruction.operand(), this.accumulator);
            case LDA -> this.accumulator = this.memory.getOrDefault(instruction.operand(), 0);
            case BRA -> this.programCounter = instruction.operand();
            case BRZ -> {
                if (this.accumulator == 0) {
                    this.programCounter = instruction.operand();
                }
            }
            case BRP -> {
                if (this.accumulator >= 0) {
                    this.programCounter = instruction.operand();
                }
            }
            case INP -> this.accumulator = this.ioHandler.readInput();
            case OUT -> this.ioHandler.pushOutput(this.accumulator);
            case OTC -> this.ioHandler.pushOutput(this.accumulator, true);
            case ERR -> throw new InterpreterException("Invalid instruction (ERR) at address " + this.programCounter + ": " + instruction);
        }
        return result;
    }
}
