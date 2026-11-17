package com.reimbursement.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ClaimFactory {

    public static final String[] TYPES = { "TRAVEL", "MEDICAL", "FOOD" };

    private ClaimFactory() {
    }

    public static Claim create(String type, Employee employee, BigDecimal amount, LocalDate expenseDate,
            String receiptNo, String description) {
        return switch (type) {
            case "TRAVEL" -> new TravelClaim(employee, amount, expenseDate, receiptNo, description);
            case "MEDICAL" -> new MedicalClaim(employee, amount, expenseDate, receiptNo, description);
            case "FOOD" -> new FoodClaim(employee, amount, expenseDate, receiptNo, description);
            default -> throw new IllegalArgumentException("Unknown claim type " + type);
        };
    }
}
