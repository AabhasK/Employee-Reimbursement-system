package com.reimbursement.exception;

public class ClaimLimitExceededException extends ReimbursementException {

    public ClaimLimitExceededException(String message) {
        super(message);
    }
}
