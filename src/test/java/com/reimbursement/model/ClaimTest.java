package com.reimbursement.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.reimbursement.exception.InvalidClaimStateException;
import com.reimbursement.exception.ReimbursementException;

class ClaimTest {

    private final Employee arjun = new Employee(4, "Arjun Singh", "arjun@company.com", "hash", Role.EMPLOYEE, 2);

    @Test
    void newClaimStartsSubmitted() {
        Claim claim = new FoodClaim(arjun, new BigDecimal("300"), LocalDate.now(), null, "Lunch");
        assertEquals(ClaimStatus.SUBMITTED, claim.getStatus());
        assertEquals(0, claim.getResubmitCount());
        assertNotNull(claim.getSubmittedAt());
    }

    @Test
    void eachTypeHasItsOwnLimit() {
        Claim travel = new TravelClaim(arjun, BigDecimal.TEN, LocalDate.now(), "T1", "Cab");
        Claim medical = new MedicalClaim(arjun, BigDecimal.TEN, LocalDate.now(), "M1", "Clinic");
        Claim food = new FoodClaim(arjun, BigDecimal.TEN, LocalDate.now(), "F1", "Tea");

        assertEquals("TRAVEL", travel.getType());
        assertEquals(new BigDecimal("25000"), travel.getLimit());
        assertEquals("MEDICAL", medical.getType());
        assertEquals(new BigDecimal("50000"), medical.getLimit());
        assertEquals("FOOD", food.getType());
        assertEquals(new BigDecimal("2000"), food.getLimit());
    }

    @Test
    void zeroAmountIsRejected() {
        Claim claim = foodOn(LocalDate.now(), "0");
        assertThrows(ReimbursementException.class, claim::validate);
    }

    @Test
    void negativeAmountIsRejected() {
        Claim claim = foodOn(LocalDate.now(), "-50");
        assertThrows(ReimbursementException.class, claim::validate);
    }

    @Test
    void futureDateIsRejected() {
        Claim claim = foodOn(LocalDate.now().plusDays(1), "100");
        assertThrows(ReimbursementException.class, claim::validate);
    }

    @Test
    void dateExactlySixtyDaysOldIsAccepted() {
        Claim claim = foodOn(LocalDate.now().minusDays(60), "100");
        assertDoesNotThrow(claim::validate);
    }

    @Test
    void dateSixtyOneDaysOldIsRejected() {
        Claim claim = foodOn(LocalDate.now().minusDays(61), "100");
        assertThrows(ReimbursementException.class, claim::validate);
    }

    @Test
    void amountWithThreeDecimalsIsRejected() {
        Claim claim = foodOn(LocalDate.now(), "100.555");
        assertThrows(ReimbursementException.class, claim::validate);
    }

    @Test
    void trailingZerosDoNotCountAsDecimals() {
        Claim claim = foodOn(LocalDate.now(), "100.500");
        assertDoesNotThrow(claim::validate);
    }

    @Test
    void tooLongDescriptionIsRejected() {
        Claim claim = new FoodClaim(arjun, new BigDecimal("100"), LocalDate.now(), "F1", "x".repeat(256));
        assertThrows(ReimbursementException.class, claim::validate);
    }

    @Test
    void tooLongReceiptIsRejected() {
        Claim claim = new FoodClaim(arjun, new BigDecimal("100"), LocalDate.now(), "R".repeat(51), "Lunch");
        assertThrows(ReimbursementException.class, claim::validate);
    }

    @Test
    void stateMessageIsReadable() {
        Claim claim = foodOn(LocalDate.now(), "100");
        claim.setId(7);
        claim.setStatus(ClaimStatus.REIMBURSED);
        InvalidClaimStateException e = assertThrows(InvalidClaimStateException.class,
                () -> claim.checkCanMoveTo(ClaimStatus.REJECTED));
        assertEquals("Claim #7 is reimbursed, so it cannot be rejected", e.getMessage());
    }

    private Claim foodOn(LocalDate date, String amount) {
        return new FoodClaim(arjun, new BigDecimal(amount), date, "F100", "Lunch");
    }
}
