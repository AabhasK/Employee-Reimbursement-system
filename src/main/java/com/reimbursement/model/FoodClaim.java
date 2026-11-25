package com.reimbursement.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.reimbursement.exception.ReimbursementException;

public class FoodClaim extends Claim {

    public static final BigDecimal STUDENT_LIMIT = new BigDecimal("1500");
    public static final BigDecimal STAFF_LIMIT = new BigDecimal("2000");
    public static final BigDecimal STUDENT_PROOF_ABOVE = new BigDecimal("200");

    public FoodClaim(Employee employee, BigDecimal amount, LocalDate expenseDate, String receiptNo, String description) {
        super(employee, amount, expenseDate, receiptNo, description);
    }

    @Override
    public void validate() throws ReimbursementException {
        checkCommonRules();
        if (needsProof()) {
            requireReceipt("Student food claims above Rs " + STUDENT_PROOF_ABOVE + " need the bill number");
        }
    }

    // Staff get a fixed daily allowance, so only students show itemized bills
    @Override
    public BigDecimal getProofAbove() {
        return isStudent() ? STUDENT_PROOF_ABOVE : null;
    }

    @Override
    public String getType() {
        return "FOOD";
    }

    @Override
    public String getTypeName() {
        return "Food and meals";
    }

    @Override
    public BigDecimal getLimit() {
        return isStudent() ? STUDENT_LIMIT : STAFF_LIMIT;
    }
}
