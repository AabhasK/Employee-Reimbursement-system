package com.reimbursement.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import com.reimbursement.exception.ClaimLimitExceededException;
import com.reimbursement.exception.DataAccessException;
import com.reimbursement.exception.DuplicateReceiptException;
import com.reimbursement.exception.InvalidClaimStateException;
import com.reimbursement.exception.MissingReceiptException;
import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.exception.SelfApprovalException;
import com.reimbursement.exception.UnauthorizedActionException;

class MessagesTest {

    @Test
    void eachCustomExceptionGetsAHeading() {
        assertEquals("Over the limit: x", Messages.describe(new ClaimLimitExceededException("x")));
        assertEquals("Receipt needed: x", Messages.describe(new MissingReceiptException("x")));
        assertEquals("Duplicate receipt: x", Messages.describe(new DuplicateReceiptException("x")));
        assertEquals("Not allowed: x", Messages.describe(new SelfApprovalException("x")));
        assertEquals("Not allowed: x", Messages.describe(new UnauthorizedActionException("x")));
        assertEquals("Wrong claim status: x", Messages.describe(new InvalidClaimStateException("x")));
    }

    @Test
    void plainRuleMessageHasNoHeading() {
        assertEquals("Amount must be more than zero",
                Messages.describe(new ReimbursementException("Amount must be more than zero")));
    }

    @Test
    void databaseProblemTellsUserWhatToCheck() {
        String text = Messages.describe(new DataAccessException("Could not load claims", new SQLException("down")));
        assertTrue(text.startsWith("Database problem: Could not load claims"));
        assertTrue(text.contains("MySQL is running"));
    }
}
