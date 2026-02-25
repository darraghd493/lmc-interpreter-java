package me.darragh.lmc.interpreter;

/**
 * A generic exception thrown by the interpreter when an error occurs during execution.
 *
 * @author darraghd493
 * @since 1.0.0
 */
public class InterpreterException extends Exception {
    public InterpreterException(String message) {
        super(message);
    }

    public InterpreterException(String message, Throwable cause) {
        super(message, cause);
    }
}
