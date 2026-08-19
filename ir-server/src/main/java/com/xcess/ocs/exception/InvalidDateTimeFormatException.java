package com.xcess.ocs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidDateTimeFormatException extends RuntimeException {

    public InvalidDateTimeFormatException(String message) {
        super(message);
    }
}