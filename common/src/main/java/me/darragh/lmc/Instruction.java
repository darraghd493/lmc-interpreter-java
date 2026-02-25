package me.darragh.lmc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author darraghd493
 * @since 1.0.0
 */
public record Instruction(@NotNull Opcode opcode, @Nullable Integer operand) {
}
