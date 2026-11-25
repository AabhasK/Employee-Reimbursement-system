package com.reimbursement.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.reimbursement.exception.ReimbursementException;

// Cabs, autos, metro and buses inside the city
public class LocalConveyanceClaim extends Claim {

    public static final BigDecimal STUDENT_LIMIT = new BigDecimal("800");
    public static final BigDecimal STAFF_LIMIT = new BigDecimal("3000");
    public static final BigDecimal PROOF_ABOVE = new BigDecimal("200");

    public LocalConveyanceClaim(Employee employee, BigDecimal amount, LocalDate expenseDate, String receiptNo,
            String description) {
        super(employee, amount, expenseDate, receiptNo, description);
    }

    @Override
    public void validate() throws ReimbursementException {
        checkCommonRules();
        if (needsProof()) {
            requireReceipt("Local conveyance claims above Rs " + PROOF_ABOVE + " need the cab or auto receipt number");
        }
    }

    @Override
    public BigDecimal getProofAbove() {
        return PROOF_ABOVE;
    }

    @Override
    public String getType() {
        return "LOCAL";
    }

    @Override
    public String getTypeName() {
        return "Local conveyance";
    }

    @Override
    public BigDecimal getLimit() {
        return isStudent() ? STUDENT_LIMIT : STAFF_LIMIT;
    }
}
