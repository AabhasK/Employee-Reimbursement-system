package com.reimbursement.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.reimbursement.exception.ReimbursementException;

public class EquipmentClaim extends Claim {

    public static final BigDecimal LIMIT = new BigDecimal("25000");

    public EquipmentClaim(Employee employee, BigDecimal amount, LocalDate expenseDate, String invoiceNo,
            String description) {
        super(employee, amount, expenseDate, invoiceNo, description);
    }

    @Override
    public void validate() throws ReimbursementException {
        checkCommonRules();
        requireReceipt("Equipment claims need the purchase invoice number");
    }

    @Override
    public String getType() {
        return "EQUIPMENT";
    }

    @Override
    public String getTypeName() {
        return "Equipment and electronics";
    }

    @Override
    public BigDecimal getLimit() {
        return LIMIT;
    }
}
