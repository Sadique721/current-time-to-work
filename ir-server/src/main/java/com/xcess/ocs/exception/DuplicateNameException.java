package com.xcess.ocs.exception;

public class DuplicateNameException extends RuntimeException {
    public DuplicateNameException(String entityType, String name) {
        super(entityType + " with name '" + name + "' already exists");
    }
}
