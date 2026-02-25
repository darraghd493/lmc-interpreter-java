package me.darragh.lmc.interpreter;

import lombok.AllArgsConstructor;
import lombok.Data;
import me.darragh.lmc.Opcode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author darraghd493
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
public class IntermediaryInstruction {
    private final @NotNull Opcode opcode;
    private @Nullable String operand;
}
