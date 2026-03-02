package me.darragh.lmc;

import org.jetbrains.annotations.NotNull;

/**
 * Stores an LMC instruction, consisting of an opcode and an optional operand.
 *
 * @author darraghd493
 * @since 1.0.0
 */
public record Instruction(@NotNull Opcode opcode, Integer operand) {
}
