package com.reimbursement.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.model.Claim;
import com.reimbursement.model.Employee;
import com.reimbursement.model.MedicalClaim;
import com.reimbursement.model.Role;

class ClaimFormTest {

    private final Employee arjun = new Employee(4, "Arjun Singh", "arjun@company.com", "h", Role.EMPLOYEE, 2);

    @Test
    void validFormBuildsTheRightClaimType() throws ReimbursementException {
        ClaimForm form = new ClaimForm("", "MEDICAL", " 1200.50 ", "2026-11-10", "M1", "Clinic");
        Claim claim = form.toNewClaim(arjun);

        assertInstanceOf(MedicalClaim.class, claim);
        assertEquals(new BigDecimal("1200.50"), claim.getAmount());
        assertEquals(LocalDate.of(2026, 11, 10), claim.getExpenseDate());
    }

    @Test
    void textAmountGivesClearError() {
        ClaimForm form = new ClaimForm("", "FOOD", "12abc", "2026-11-10", "", "");
        assertThrows(ReimbursementException.class, form::toAmount);
    }

    @Test
    void emptyAmountGivesClearError() {
        ClaimForm form = new ClaimForm("", "FOOD", "", "2026-11-10", "", "");
        assertThrows(ReimbursementException.class, form::toAmount);
    }

    @Test
    void unknownTypeIsRejected() {
        ClaimForm form = new ClaimForm("", "HOTEL", "100", "2026-11-10", "", "");
        assertThrows(ReimbursementException.class, () -> form.toNewClaim(arjun));
    }

    @Test
    void emptyDateIsLeftForValidateToReport() throws ReimbursementException {
        ClaimForm form = new ClaimForm("", "FOOD", "100", "", "", "");
        assertNull(form.toExpenseDate());
    }

    @Test
    void badDateGivesClearError() {
        ClaimForm form = new ClaimForm("", "FOOD", "100", "17/11/2026", "", "");
        assertThrows(ReimbursementException.class, form::toExpenseDate);
    }

    @Test
    void claimIdDecidesSubmitOrResubmit() throws ReimbursementException {
        assertFalse(ClaimForm.empty().isEditing());
        ClaimForm editing = new ClaimForm("12", "FOOD", "100", "2026-11-10", "", "");
        assertTrue(editing.isEditing());
        assertEquals(12, editing.toClaimId());
    }

    @Test
    void tamperedClaimIdGivesClearError() {
        ClaimForm form = new ClaimForm("12; DROP TABLE claims", "FOOD", "100", "2026-11-10", "", "");
        assertThrows(ReimbursementException.class, form::toClaimId);
    }
}
