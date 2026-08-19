package com.xcess.ocs.exception;

public class ForeignReferenceException extends RuntimeException{
    public ForeignReferenceException(String message){
        super(message);
    }
}
