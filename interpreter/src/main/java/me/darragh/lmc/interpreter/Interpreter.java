package me.darragh.lmc.interpreter;

import lombok.RequiredArgsConstructor;
import me.darragh.lmc.Instruction;
import me.darragh.lmc.Opcode;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class Interpreter {
    private final @NotNull Instruction[] instructions;
    private final @NotNull IoHandler ioHandler;

    private final Map<Integer, Integer> memory = new HashMap<>();

    private int programCounter = 0, accumulator = 0;

    public void prepare() {
        for (int i = 0; i < this.instructions.length; i++) {
            Instruction instruction = this.instructions[i];
            if (instruction.opcode() == Opcode.DAT) {
                this.memory.put(i, instruction.operand());
            }
        }
    }

    public void run() {
        this.prepare();
        //noinspection StatementWithEmptyBody
        while (this.step()) {
        }
    }

    public boolean step() {
        if (this.programCounter >= this.instructions.length) {
            throw new RuntimeException("Program counter out of bounds");
        }

        Instruction instruction = this.instructions[this.programCounter];
        boolean result = this.execute(instruction);
        this.programCounter++;

        return result;
    }

    private boolean execute(@NotNull Instruction instruction) {
        boolean result = true;
        switch (instruction.opcode()) {
            case HLT -> result = false;
            case ADD -> this.accumulator = this.memory.getOrDefault(instruction.operand(), 0) + this.accumulator;
            case SUB -> this.accumulator = this.memory.getOrDefault(instruction.operand(), 0) - this.accumulator;
            case STA, STO -> this.memory.put(instruction.operand(), this.accumulator);
            case LDA -> this.accumulator = this.memory.getOrDefault(instruction.operand(), 0);
            case BRA -> this.programCounter = instruction.operand() - 1;
            case BRZ -> {
                if (this.accumulator == 0) {
                    this.programCounter = instruction.operand() - 1;
                }
            }
            case BRP -> {
                if (this.accumulator >= 0) {
                    this.programCounter = instruction.operand() - 1;
                }
            }
            case INP -> this.accumulator = this.ioHandler.readInput();
            case OUT -> this.ioHandler.pushOutput(this.accumulator);
            case OTC -> this.ioHandler.pushOutput(this.accumulator, true);
        }
        return result;
    }
}
