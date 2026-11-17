package com.reimbursement.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.reimbursement.exception.ClaimLimitExceededException;
import com.reimbursement.exception.MissingReceiptException;

class TravelClaimTest {

    private final Employee arjun = new Employee(4, "Arjun Singh", "arjun@company.com", "hash", Role.EMPLOYEE, 2);

    @Test
    void amountAtLimitIsAccepted() {
        assertDoesNotThrow(travel("25000", "T1")::validate);
    }

    @Test
    void amountAboveLimitIsRejected() {
        assertThrows(ClaimLimitExceededException.class, travel("25000.01", "T1")::validate);
    }

    @Test
    void noReceiptNeededAtThousand() {
        assertDoesNotThrow(travel("1000", null)::validate);
    }

    @Test
    void receiptNeededAboveThousand() {
        assertThrows(MissingReceiptException.class, travel("1000.01", null)::validate);
    }

    @Test
    void messagesNameTheRule() {
        ClaimLimitExceededException limit = assertThrows(ClaimLimitExceededException.class,
                travel("30000", "T1")::validate);
        assertEquals("Travel claims can be at most Rs 25000. This one is Rs 30000", limit.getMessage());

        MissingReceiptException receipt = assertThrows(MissingReceiptException.class,
                travel("1500", null)::validate);
        assertEquals("Travel claims above Rs 1000 need a receipt number", receipt.getMessage());
    }

    @Test
    void blankReceiptCountsAsMissing() {
        assertThrows(MissingReceiptException.class, travel("5000", "   ")::validate);
    }

    private Claim travel(String amount, String receipt) {
        return new TravelClaim(arjun, new BigDecimal(amount), LocalDate.now(), receipt, "Cab to client office");
    }
}
