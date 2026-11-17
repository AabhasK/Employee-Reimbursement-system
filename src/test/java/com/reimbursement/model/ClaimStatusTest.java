package com.reimbursement.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClaimStatusTest {

    @Test
    void legalMovesAreAllowed() {
        assertTrue(ClaimStatus.SUBMITTED.canMoveTo(ClaimStatus.APPROVED));
        assertTrue(ClaimStatus.SUBMITTED.canMoveTo(ClaimStatus.REJECTED));
        assertTrue(ClaimStatus.APPROVED.canMoveTo(ClaimStatus.REIMBURSED));
        assertTrue(ClaimStatus.REJECTED.canMoveTo(ClaimStatus.SUBMITTED));
    }

    @Test
    void everyOtherMoveIsBlocked() {
        int allowed = 0;
        for (ClaimStatus from : ClaimStatus.values()) {
            for (ClaimStatus to : ClaimStatus.values()) {
                if (from.canMoveTo(to)) {
                    allowed++;
                }
            }
        }
        assertEquals(4, allowed);
    }

    @Test
    void reimbursedIsFinal() {
        for (ClaimStatus to : ClaimStatus.values()) {
            assertFalse(ClaimStatus.REIMBURSED.canMoveTo(to));
        }
    }

    @Test
    void approvedCannotGoBackToRejected() {
        assertFalse(ClaimStatus.APPROVED.canMoveTo(ClaimStatus.REJECTED));
    }
}
