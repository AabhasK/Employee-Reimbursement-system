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
    private final Employee riya = new Employee(8, "Riya Sharma", "riya@company.com", "hash", Role.EMPLOYEE, 2,
            Grade.STUDENT);

    @Test
    void staffAmountAtLimitIsAccepted() {
        assertDoesNotThrow(travel(arjun, "25000", "T1")::validate);
    }

    @Test
    void staffAmountAboveLimitIsRejected() {
        assertThrows(ClaimLimitExceededException.class, travel(arjun, "25000.01", "T1")::validate);
    }

    @Test
    void studentLimitIsLower() {
        assertDoesNotThrow(travel(riya, "6000", "T1")::validate);
        assertThrows(ClaimLimitExceededException.class, travel(riya, "6000.01", "T1")::validate);
    }

    @Test
    void ticketIsAlwaysNeeded() {
        assertThrows(MissingReceiptException.class, travel(arjun, "100", null)::validate);
        assertThrows(MissingReceiptException.class, travel(arjun, "5000", "   ")::validate);
    }

    @Test
    void messagesNameTheGradeAndRule() {
        ClaimLimitExceededException limit = assertThrows(ClaimLimitExceededException.class,
                travel(arjun, "30000", "T1")::validate);
        assertEquals("For staff, inter-city travel claims can be at most Rs 25000. This one is Rs 30000",
                limit.getMessage());

        MissingReceiptException receipt = assertThrows(MissingReceiptException.class,
                travel(riya, "1500", null)::validate);
        assertEquals("Inter-city travel claims always need the ticket or PNR number", receipt.getMessage());
    }

    private Claim travel(Employee owner, String amount, String receipt) {
        return new TravelClaim(owner, new BigDecimal(amount), LocalDate.now(), receipt, "Train to Delhi");
    }
}
