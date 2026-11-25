package com.reimbursement.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.reimbursement.exception.ClaimLimitExceededException;
import com.reimbursement.exception.MissingReceiptException;

class FoodClaimTest {

    private final Employee arjun = new Employee(4, "Arjun Singh", "arjun@company.com", "hash", Role.EMPLOYEE, 2);
    private final Employee riya = new Employee(8, "Riya Sharma", "riya@company.com", "hash", Role.EMPLOYEE, 2,
            Grade.STUDENT);

    @Test
    void staffNeverNeedABillBecauseOfDailyAllowance() {
        assertDoesNotThrow(food(arjun, "2000", null)::validate);
        assertFalse(food(arjun, "2000", null).needsProof());
    }

    @Test
    void studentNeedsBillAboveTwoHundred() {
        assertDoesNotThrow(food(riya, "200", null)::validate);
        assertThrows(MissingReceiptException.class, food(riya, "200.01", null)::validate);
        assertTrue(food(riya, "200.01", "F1").needsProof());
    }

    @Test
    void limitsDependOnGrade() {
        assertDoesNotThrow(food(arjun, "2000", null)::validate);
        assertThrows(ClaimLimitExceededException.class, food(arjun, "2001", null)::validate);
        assertDoesNotThrow(food(riya, "1500", "F1")::validate);
        assertThrows(ClaimLimitExceededException.class, food(riya, "1501", "F1")::validate);
    }

    private Claim food(Employee owner, String amount, String receipt) {
        return new FoodClaim(owner, new BigDecimal(amount), LocalDate.now(), receipt, "Team lunch");
    }
}
