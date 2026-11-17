package com.reimbursement.model;

public enum ClaimStatus {
    SUBMITTED,
    APPROVED,
    REJECTED,
    REIMBURSED;

    public boolean canMoveTo(ClaimStatus next) {
        return switch (this) {
            case SUBMITTED -> next == APPROVED || next == REJECTED;
            case APPROVED -> next == REIMBURSED;
            case REJECTED -> next == SUBMITTED;
            case REIMBURSED -> false;
        };
    }
}
