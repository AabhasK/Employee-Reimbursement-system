package com.reimbursement.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.reimbursement.exception.ReimbursementException;

public class TravelClaim extends Claim {

    public static final BigDecimal LIMIT = new BigDecimal("25000");
    public static final BigDecimal RECEIPT_ABOVE = new BigDecimal("1000");

    public TravelClaim(Employee employee, BigDecimal amount, LocalDate expenseDate, String receiptNo, String description) {
        super(employee, amount, expenseDate, receiptNo, description);
    }

    @Override
    public void validate() throws ReimbursementException {
        checkCommonRules();
        if (getAmount().compareTo(RECEIPT_ABOVE) > 0) {
            requireReceipt("Travel claims above Rs " + RECEIPT_ABOVE + " need a receipt number");
        }
    }

    @Override
    public String getType() {
        return "TRAVEL";
    }

    @Override
    public BigDecimal getLimit() {
        return LIMIT;
    }
}
