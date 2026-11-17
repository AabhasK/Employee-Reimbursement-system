package com.reimbursement.exception;

public class ReimbursementException extends Exception {

    public ReimbursementException(String message) {
        super(message);
    }

    public ReimbursementException(String message, Throwable cause) {
        super(message, cause);
    }
}
