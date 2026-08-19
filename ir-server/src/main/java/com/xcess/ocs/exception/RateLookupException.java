package com.xcess.ocs.exception;

public class RateLookupException extends RuntimeException {
    public RateLookupException(String message) {
        super(message);
    }

    public RateLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}
