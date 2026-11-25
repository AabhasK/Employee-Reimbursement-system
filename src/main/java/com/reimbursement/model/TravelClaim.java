package com.reimbursement.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.reimbursement.exception.ReimbursementException;

// Inter-city travel: train, bus or flight tickets
public class TravelClaim extends Claim {

    public static final BigDecimal STUDENT_LIMIT = new BigDecimal("6000");
    public static final BigDecimal STAFF_LIMIT = new BigDecimal("25000");

    public TravelClaim(Employee employee, BigDecimal amount, LocalDate expenseDate, String receiptNo, String description) {
        super(employee, amount, expenseDate, receiptNo, description);
    }

    @Override
    public void validate() throws ReimbursementException {
        checkCommonRules();
        requireReceipt("Inter-city travel claims always need the ticket or PNR number");
    }

    @Override
    public String getType() {
        return "TRAVEL";
    }

    @Override
    public String getTypeName() {
        return "Inter-city travel";
    }

    @Override
    public BigDecimal getLimit() {
        return isStudent() ? STUDENT_LIMIT : STAFF_LIMIT;
    }
}
