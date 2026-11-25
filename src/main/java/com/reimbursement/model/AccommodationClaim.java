package com.reimbursement.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.reimbursement.exception.ReimbursementException;

public class AccommodationClaim extends Claim {

    public static final BigDecimal STUDENT_LIMIT = new BigDecimal("2500");
    public static final BigDecimal STAFF_LIMIT = new BigDecimal("12000");

    public AccommodationClaim(Employee employee, BigDecimal amount, LocalDate expenseDate, String receiptNo,
            String description) {
        super(employee, amount, expenseDate, receiptNo, description);
    }

    @Override
    public void validate() throws ReimbursementException {
        checkCommonRules();
        requireReceipt("Accommodation claims always need the hotel or hostel invoice number");
    }

    @Override
    public String getType() {
        return "ACCOMMODATION";
    }

    @Override
    public BigDecimal getLimit() {
        return isStudent() ? STUDENT_LIMIT : STAFF_LIMIT;
    }
}
