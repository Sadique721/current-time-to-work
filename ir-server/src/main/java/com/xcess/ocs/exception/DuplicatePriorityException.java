package com.xcess.ocs.exception;

public class DuplicatePriorityException extends RuntimeException {
    
    public DuplicatePriorityException(String message) {
        super(message);
    }
    
    public DuplicatePriorityException(String entityName, String fieldName) {
        super(String.format("Duplicate priority values not allowed in %s for field %s", entityName, fieldName));
    }
}