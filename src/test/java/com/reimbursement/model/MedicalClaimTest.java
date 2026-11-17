package com.reimbursement.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.reimbursement.exception.ClaimLimitExceededException;
import com.reimbursement.exception.MissingReceiptException;

class MedicalClaimTest {

    private final Employee arjun = new Employee(4, "Arjun Singh", "arjun@company.com", "hash", Role.EMPLOYEE, 2);

    @Test
    void smallClaimStillNeedsReceipt() {
        assertThrows(MissingReceiptException.class, medical("100", null)::validate);
    }

    @Test
    void amountAtLimitIsAccepted() {
        assertDoesNotThrow(medical("50000", "M1")::validate);
    }

    @Test
    void amountAboveLimitIsRejected() {
        assertThrows(ClaimLimitExceededException.class, medical("50001", "M1")::validate);
    }

    private Claim medical(String amount, String receipt) {
        return new MedicalClaim(arjun, new BigDecimal(amount), LocalDate.now(), receipt, "Clinic visit");
    }
}
