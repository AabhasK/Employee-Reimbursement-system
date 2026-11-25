package com.reimbursement.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.reimbursement.exception.ReimbursementException;

// Registration fees, delegate passes, prototyping hardware and printing
public class EventClaim extends Claim {

    public static final BigDecimal STUDENT_LIMIT = new BigDecimal("10000");
    public static final BigDecimal STAFF_LIMIT = new BigDecimal("20000");

    public EventClaim(Employee employee, BigDecimal amount, LocalDate expenseDate, String receiptNo,
            String description) {
        super(employee, amount, expenseDate, receiptNo, description);
    }

    @Override
    public void validate() throws ReimbursementException {
        checkCommonRules();
        requireReceipt("Event and material claims always need the fee receipt or bill number");
    }

    @Override
    public String getType() {
        return "EVENT";
    }

    @Override
    public String getTypeName() {
        return "Event and materials";
    }

    @Override
    public BigDecimal getLimit() {
        return isStudent() ? STUDENT_LIMIT : STAFF_LIMIT;
    }
}
