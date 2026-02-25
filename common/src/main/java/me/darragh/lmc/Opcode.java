package me.darragh.lmc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

/**
 * An enum representing the opcodes of the LMC assembly language.
 *
 * @author darraghd493
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum Opcode {
    HLT("HLT"),
    ADD("ADD"),
    SUB("SUB"),
    STA("STA"),
    STO("STO"),
    ERR("ERR"),
    LDA("LDA"),
    BRA("BRA"),
    BRZ("BRZ"),
    BRP("BRP"),
    INP("INP"),
    OUT("OUT"),
    OTC("OTC"),
    DAT("DAT");

    private final String opcodeString;

    public static @Nullable Opcode fromString(String opcodeString) {
        for (Opcode opcode : values()) {
            if (opcode.getOpcodeString().equalsIgnoreCase(opcodeString)) {
                return opcode;
            }
        }
        return null;
    }
}
