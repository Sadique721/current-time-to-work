package com.xcess.ocs.exception;

public class XmlConversionException extends RuntimeException {
    public XmlConversionException(String message) {
        super(message);
    }

    public XmlConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
