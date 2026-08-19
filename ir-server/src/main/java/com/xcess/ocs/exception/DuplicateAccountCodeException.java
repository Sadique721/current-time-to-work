package com.xcess.ocs.exception;

public class DuplicateAccountCodeException extends RuntimeException {
    public DuplicateAccountCodeException(String accountCode) {
        super("Account code '" + accountCode + "' already exists");
    }

    public DuplicateAccountCodeException(String partnerName,String message)
    {
        super(message+" "+partnerName);
    }
}