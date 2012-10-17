package org.unicode.cldr.util;

public class InternalCldrException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InternalCldrException(String message) {
        super(message);
    }

    public InternalCldrException(String message, Throwable cause) {
        super(message, cause);
    }
}