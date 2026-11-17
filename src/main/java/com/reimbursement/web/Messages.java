package com.reimbursement.web;

import com.reimbursement.exception.ClaimLimitExceededException;
import com.reimbursement.exception.DataAccessException;
import com.reimbursement.exception.DuplicateReceiptException;
import com.reimbursement.exception.InvalidClaimStateException;
import com.reimbursement.exception.MissingReceiptException;
import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.exception.SelfApprovalException;
import com.reimbursement.exception.UnauthorizedActionException;

class Messages {

    private Messages() {
    }

    static String describe(ReimbursementException e) {
        String heading = switch (e) {
            case ClaimLimitExceededException limit -> "Over the limit";
            case MissingReceiptException missing -> "Receipt needed";
            case DuplicateReceiptException duplicate -> "Duplicate receipt";
            case SelfApprovalException self -> "Not allowed";
            case UnauthorizedActionException unauthorized -> "Not allowed";
            case InvalidClaimStateException state -> "Wrong claim status";
            case DataAccessException data -> "Database problem";
            default -> null;
        };
        String text = heading == null ? e.getMessage() : heading + ": " + e.getMessage();
        if (e instanceof DataAccessException) {
            text += ". Check that MySQL is running and db.properties is correct";
        }
        return text;
    }
}
