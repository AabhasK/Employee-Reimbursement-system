package com.reimbursement.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.reimbursement.exception.ClaimLimitExceededException;
import com.reimbursement.exception.MissingReceiptException;

class FoodClaimTest {

    private final Employee arjun = new Employee(4, "Arjun Singh", "arjun@company.com", "hash", Role.EMPLOYEE, 2);

    @Test
    void noReceiptNeededAtFiveHundred() {
        assertDoesNotThrow(food("500", null)::validate);
    }

    @Test
    void receiptNeededAboveFiveHundred() {
        assertThrows(MissingReceiptException.class, food("500.01", null)::validate);
    }

    @Test
    void amountAtLimitIsAccepted() {
        assertDoesNotThrow(food("2000", "F1")::validate);
    }

    @Test
    void amountAboveLimitIsRejected() {
        assertThrows(ClaimLimitExceededException.class, food("2001", "F1")::validate);
    }

    private Claim food(String amount, String receipt) {
        return new FoodClaim(arjun, new BigDecimal(amount), LocalDate.now(), receipt, "Team lunch");
    }
}
