package me.darragh.lmc.interpreter.utility;

import org.jetbrains.annotations.Nullable;

public class LanguageUtility {
    public static @Nullable Integer parse(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
