package org.snapitch.contaistner;

public class ContaistnerException extends RuntimeException {

    public ContaistnerException(String message) {
        super(message);
    }

    public ContaistnerException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContaistnerException(Throwable cause) {
        super(cause);
    }
}
