package de.waterdu.atlantis.mod.wrapper;

import java.io.Serial;

/**
 * Thrown when code dereferences a {@link ModWrapper} that has no object to give, such as an event wrapper.
 */
public class IllegalDereferenceException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -6174568513074740347L;

    /**
     * Creates the exception with no message.
     */
    public IllegalDereferenceException() {
    }

    /**
     * Creates the exception with a message.
     *
     * @param s the message
     */
    public IllegalDereferenceException(String s) {
        super(s);
    }

    /**
     * Creates the exception with a message and a cause.
     *
     * @param message the message
     * @param cause the cause
     */
    public IllegalDereferenceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates the exception with a cause.
     *
     * @param cause the cause
     */
    public IllegalDereferenceException(Throwable cause) {
        super(cause);
    }
}
