package com.reimbursement.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.reimbursement.exception.ReimbursementException;

public class MedicalClaim extends Claim {

    public static final BigDecimal LIMIT = new BigDecimal("50000");

    public MedicalClaim(Employee employee, BigDecimal amount, LocalDate expenseDate, String receiptNo, String description) {
        super(employee, amount, expenseDate, receiptNo, description);
    }

    @Override
    public void validate() throws ReimbursementException {
        checkCommonRules();
        requireReceipt("Medical claims always need a receipt number");
    }

    @Override
    public String getType() {
        return "MEDICAL";
    }

    @Override
    public BigDecimal getLimit() {
        return LIMIT;
    }
}
