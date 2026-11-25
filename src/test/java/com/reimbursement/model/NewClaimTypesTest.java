package com.reimbursement.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.reimbursement.exception.ClaimLimitExceededException;
import com.reimbursement.exception.MissingReceiptException;

class NewClaimTypesTest {

    private final Employee arjun = new Employee(4, "Arjun Singh", "arjun@company.com", "hash", Role.EMPLOYEE, 2);
    private final Employee riya = new Employee(8, "Riya Sharma", "riya@company.com", "hash", Role.EMPLOYEE, 2,
            Grade.STUDENT);

    @Test
    void accommodationLimitsAndInvoice() {
        assertDoesNotThrow(claim("ACCOMMODATION", arjun, "12000", "H1")::validate);
        assertThrows(ClaimLimitExceededException.class, claim("ACCOMMODATION", riya, "2500.01", "H1")::validate);
        assertThrows(MissingReceiptException.class, claim("ACCOMMODATION", arjun, "500", null)::validate);
    }

    @Test
    void localConveyanceNeedsReceiptAboveTwoHundred() {
        assertDoesNotThrow(claim("LOCAL", riya, "200", null)::validate);
        assertThrows(MissingReceiptException.class, claim("LOCAL", riya, "250", null)::validate);
        assertFalse(claim("LOCAL", arjun, "200", null).needsProof());
        assertTrue(claim("LOCAL", arjun, "201", "C1").needsProof());
    }

    @Test
    void localConveyanceLimitsDependOnGrade() {
        assertDoesNotThrow(claim("LOCAL", arjun, "3000", "C1")::validate);
        assertThrows(ClaimLimitExceededException.class, claim("LOCAL", riya, "801", "C1")::validate);
    }

    @Test
    void eventFeesAlwaysNeedProof() {
        assertDoesNotThrow(claim("EVENT", riya, "10000", "E1")::validate);
        assertThrows(ClaimLimitExceededException.class, claim("EVENT", riya, "10001", "E1")::validate);
        assertThrows(MissingReceiptException.class, claim("EVENT", arjun, "300", null)::validate);
    }

    @Test
    void factoryBuildsEveryTypeWithAReadableName() {
        assertEquals(7, ClaimFactory.TYPES.length);
        for (String type : ClaimFactory.TYPES) {
            Claim claim = claim(type, arjun, "100", "R1");
            assertEquals(type, claim.getType());
            assertEquals(ClaimFactory.nameOf(type), claim.getTypeName());
        }
        assertInstanceOf(LocalConveyanceClaim.class, claim("LOCAL", arjun, "100", null));
    }

    @Test
    void companyEquipmentHasAnInvoiceAndLimit() {
        Claim equipment = claim("EQUIPMENT", arjun, "25000", "INV-1");
        assertDoesNotThrow(equipment::validate);
        assertTrue(equipment.needsProof());
        assertEquals(ProofKind.EQUIPMENT_INVOICE, equipment.getReceiptKind());
        assertThrows(MissingReceiptException.class, claim("EQUIPMENT", arjun, "1000", null)::validate);
        assertThrows(ClaimLimitExceededException.class, claim("EQUIPMENT", arjun, "25000.01", "INV-1")::validate);
    }

    private Claim claim(String type, Employee owner, String amount, String receipt) {
        return ClaimFactory.create(type, owner, new BigDecimal(amount), LocalDate.now(), receipt, "Test");
    }
}
