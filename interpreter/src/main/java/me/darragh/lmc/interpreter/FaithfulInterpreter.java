package me.darragh.lmc.interpreter;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import me.darragh.lmc.Instruction;
import me.darragh.lmc.Opcode;
import org.jetbrains.annotations.NotNull;

/**
 * A faithful interpreter for LMC. It does faithfully follow the fetch-decode-execute cycle,
 * encodes/decodes instructions, and stores everything in memory.
 *
 * @author darraghd493
 * @since 1.0.0
 */
@Slf4j
@Builder
public class FaithfulInterpreter implements Interpreter {
    private final @NotNull Instruction[] instructions;
    private final @NotNull IoHandler ioHandler;

    @Builder.Default
    private final boolean followThreeDigitLimit = true;

    @Builder.Default
    private final boolean followMemorySizeLimit = true;

    private int[] memory;
    private int programCounter, accumulator;

    @Override
    public void prepare() {
        if (this.followMemorySizeLimit) {
            this.memory = new int[100];
        } else {
            this.memory = new int[this.instructions.length];
        }

        for (int i = 0; i < this.instructions.length; i++) {
            Instruction instruction = this.instructions[i];
            if (instruction.opcode() == Opcode.DAT) {
                this.memory[i] = this.handleValue(instruction.operand());
                continue;
            }
            this.memory[i] = encodeInstruction(this.instructions[i]);
        }
    }

    @Override
    public boolean step() throws InterpreterException {
        // fetch
        int encodedInstruction = this.memory[this.programCounter++];

        // decode
        int opcodeValue = encodedInstruction / 100;
        int operand = encodedInstruction % 100;
        Opcode opcode = Opcode.values()[opcodeValue];

        // execute
        return this.execute(opcode, operand);
    }

    private boolean execute(@NotNull Opcode opcode, int operand) throws InterpreterException {
        boolean result = true;
        switch (opcode) {
            case HLT -> result = false;
            case ADD -> this.accumulator = this.handleValue(this.accumulator + this.memory[operand]);
            case SUB -> this.accumulator = this.handleValue(this.accumulator - this.memory[operand]);
            case STA, STO -> this.memory[operand] = this.accumulator;
            case LDA -> this.accumulator = this.memory[operand];
            case BRA -> this.programCounter = operand;
            case BRZ -> {
                if (this.accumulator == 0) {
                    this.programCounter = operand;
                }
            }
            case BRP -> {
                if (this.accumulator >= 0) {
                    this.programCounter = operand;
                }
            }
            case INP -> this.accumulator = this.handleValue(this.ioHandler.readInput());
            case OUT -> this.ioHandler.pushOutput(this.accumulator);
            case OTC -> this.ioHandler.pushOutput(this.accumulator, true);
            case ERR -> throw new InterpreterException("Invalid instruction (ERR) at address %s: %s, %s".formatted(
                    this.programCounter,
                    opcode,
                    operand
            ));
        }
        return result;
    }

    private int handleValue(int value) {
        if (this.followThreeDigitLimit) {
            while (value > 999) value -= 1000;
            while (value < -999) value +=    1000;
            return value;
        }
        return value;
    }

    private static int encodeInstruction(@NotNull Instruction instruction) {
        return instruction.opcode().ordinal() * 100 + (instruction.operand() == null ? 0 : instruction.operand());
    }
}
