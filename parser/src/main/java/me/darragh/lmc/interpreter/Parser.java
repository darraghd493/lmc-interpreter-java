package me.darragh.lmc.interpreter;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import me.darragh.lmc.Instruction;
import me.darragh.lmc.Opcode;
import me.darragh.lmc.interpreter.utility.LanguageUtility;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Parses the LMC assembly source code into an array of instructions.
 *
 * @author darraghd493
 * @since 1.0.0
 */
@Slf4j
@Builder
public class Parser {
    private static final Pattern ANY_WHITESPACE = Pattern.compile("\\s+");

    @Builder.Default
    private boolean comments = true;
    @Builder.Default
    private String commentPrefix = "//";

    public @NotNull Instruction[] parse(@NotNull String source) {
        // Step 1: build a list of intermediary instructions
        String[] lines = source.split("\n");

        List<IntermediaryInstruction> intermediaryInstructions = new ArrayList<>();
        Map<String, Integer> labelLocations = new HashMap<>();

        for (String line : lines) {
            String content = this.comments ? line.split(this.commentPrefix)[0] : line.trim();
            if (content.isEmpty()) {
                continue;
            }

            // attempt to parse line
            String[] segments = ANY_WHITESPACE.split(content.trim());
            if (segments.length == 0) continue;

            switch (segments.length) {
                case 1 -> {
                    String opcodeString = segments[0].trim();
                    Opcode opcode = Opcode.fromString(opcodeString);
                    if (opcode == null) {
                        log.warn("Failed to parse opcode: {}", segments[0]);
                        intermediaryInstructions.add(new IntermediaryInstruction(Opcode.ERR, null));
                    } else {
                        intermediaryInstructions.add(new IntermediaryInstruction(opcode, null));
                    }
                }
                case 2 -> {
                    String segment0 = segments[0].trim();
                    String segment1 = segments[1].trim();

                    Opcode opcode0 = Opcode.fromString(segment0);
                    Opcode opcode1 = Opcode.fromString(segment1);

                    if (opcode0 != null) {
                        // opcode operand
                        intermediaryInstructions.add(new IntermediaryInstruction(opcode0, segment1));
                    } else if (opcode1 != null) {
                        // label opcode
                        intermediaryInstructions.add(new IntermediaryInstruction(opcode1, null));
                        labelLocations.put(segment0, intermediaryInstructions.size() - 1);
                    } else {
                        log.warn("Neither segment is a valid opcode: {} {}", segment0, segment1);
                        intermediaryInstructions.add(new IntermediaryInstruction(Opcode.ERR, null));
                    }
                }
                case 3 -> {
                    String labelString = segments[0].trim(),
                            opcodeString = segments[1].trim(),
                            operandString = segments[2].trim();
                    Opcode opcode = Opcode.fromString(opcodeString);
                    if (opcode == null) {
                        log.warn("Failed to parse opcode: {}", opcodeString);
                        intermediaryInstructions.add(new IntermediaryInstruction(Opcode.ERR, null));
                        continue;
                    }

                    intermediaryInstructions.add(new IntermediaryInstruction(opcode, operandString));
                    labelLocations.put(labelString, intermediaryInstructions.size() - 1);
                }
                default -> log.warn("Failed to parse: {}", content);
            }
        }

        // Step 2: resolve label locations
        List<Instruction> instructions = new ArrayList<>();
        for (IntermediaryInstruction instruction : intermediaryInstructions) {
            if (instruction.getOperand() == null) {
                instructions.add(new Instruction(instruction.getOpcode(), null));
                continue;
            }
            Integer operandInt = LanguageUtility.parse(instruction.getOperand());
            if (operandInt == null) {
                operandInt = labelLocations.get(instruction.getOperand());
            }
            if (operandInt == null) {
                log.warn("Failed to resolve operand: {}", instruction.getOperand());
                instructions.add(new Instruction(Opcode.ERR, null));
            } else {
                instructions.add(new Instruction(instruction.getOpcode(), operandInt));
            }
        }

        return instructions.toArray(new Instruction[0]);
    }
}
