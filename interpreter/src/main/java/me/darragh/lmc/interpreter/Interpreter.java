package me.darragh.lmc.interpreter;

/**
 * The main interface for all LMC interpreters.
 *
 * @author darraghd493
 * @since 1.0.0
 */
public interface Interpreter {
    void prepare();

    default void run() throws InterpreterException {
        this.prepare();
        //noinspection StatementWithEmptyBody
        while (this.step()) {
        }
    }

    boolean step() throws InterpreterException;
}
