package de.waterdu.atlantis.mod.wrapper;

import java.io.Serial;

public class IllegalDereferenceException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -6174568513074740347L;

    public IllegalDereferenceException() {
    }

    public IllegalDereferenceException(String s) {
        super(s);
    }

    public IllegalDereferenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalDereferenceException(Throwable cause) {
        super(cause);
    }
}
