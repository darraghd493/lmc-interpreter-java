package me.darragh.lmc.interpreter;

/**
 * Handles the I/O operations for an LMC interpreter.
 *
 * @author darraghd493
 * @since 1.0.0
 */
public interface IoHandler {
    int readInput();

    void pushOutput(int value, boolean character);

    default void pushOutput(int value) {
        this.pushOutput(value, false);
    }

    static IoHandler of(InputHandler inputHandler, OutputHandler outputHandler) {
        return new IoHandler() {
            @Override
            public int readInput() {
                return inputHandler.handle();
            }

            @Override
            public void pushOutput(int value, boolean character) {
                outputHandler.handle(value, character);
            }
        };
    }

    @FunctionalInterface
    interface InputHandler {
        int handle();
    }

    @FunctionalInterface
    interface OutputHandler {
        void handle(int value, boolean character);
    }
}
