package com.reimbursement.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClaimFactory {

    public static final String[] TYPES = { "TRAVEL", "ACCOMMODATION", "FOOD", "LOCAL", "EVENT", "EQUIPMENT", "MEDICAL" };

    private ClaimFactory() {
    }

    public static Claim create(String type, Employee employee, BigDecimal amount, LocalDate expenseDate,
            String receiptNo, String description) {
        return switch (type) {
            case "TRAVEL" -> new TravelClaim(employee, amount, expenseDate, receiptNo, description);
            case "ACCOMMODATION" -> new AccommodationClaim(employee, amount, expenseDate, receiptNo, description);
            case "FOOD" -> new FoodClaim(employee, amount, expenseDate, receiptNo, description);
            case "LOCAL" -> new LocalConveyanceClaim(employee, amount, expenseDate, receiptNo, description);
            case "EVENT" -> new EventClaim(employee, amount, expenseDate, receiptNo, description);
            case "EQUIPMENT" -> new EquipmentClaim(employee, amount, expenseDate, receiptNo, description);
            case "MEDICAL" -> new MedicalClaim(employee, amount, expenseDate, receiptNo, description);
            default -> throw new IllegalArgumentException("Unknown claim type " + type);
        };
    }

    public static List<String> typesFor(Employee employee) {
        List<String> types = new ArrayList<>();
        for (String type : TYPES) {
            if (allowsNewType(type, employee)) {
                types.add(type);
            }
        }
        return types;
    }

    public static boolean allowsNewType(String type, Employee employee) {
        boolean companyStaff = employee.getGrade() == Grade.STAFF && !employee.isUniversity();
        return switch (type) {
            case "EVENT" -> !companyStaff;
            case "EQUIPMENT" -> companyStaff;
            default -> Arrays.asList(TYPES).contains(type);
        };
    }

    public static String nameOf(String type) {
        return switch (type) {
            case "TRAVEL" -> "Inter-city travel";
            case "ACCOMMODATION" -> "Accommodation";
            case "FOOD" -> "Food and meals";
            case "LOCAL" -> "Local conveyance";
            case "EVENT" -> "Event and materials";
            case "EQUIPMENT" -> "Equipment and electronics";
            case "MEDICAL" -> "Medical";
            default -> type;
        };
    }
}
