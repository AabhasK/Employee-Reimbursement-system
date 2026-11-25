package com.reimbursement.model;

public enum ProofKind {
    TICKET("Ticket or boarding pass"),
    HOTEL_INVOICE("Hotel or hostel invoice"),
    TRANSIT_RECEIPT("Cab, auto or metro receipt"),
    FOOD_BILL("Itemized food bill"),
    FEE_RECEIPT("Fee receipt or certificate"),
    EQUIPMENT_INVOICE("Equipment purchase invoice"),
    MEDICAL_BILL("Medical bill or prescription"),
    APPROVAL_LETTER("Approval letter from HoD or Dean"),
    OTHER("Other document");

    private final String label;

    ProofKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ProofKind forClaimType(String type) {
        return switch (type) {
            case "TRAVEL" -> TICKET;
            case "ACCOMMODATION" -> HOTEL_INVOICE;
            case "FOOD" -> FOOD_BILL;
            case "LOCAL" -> TRANSIT_RECEIPT;
            case "EVENT" -> FEE_RECEIPT;
            case "EQUIPMENT" -> EQUIPMENT_INVOICE;
            case "MEDICAL" -> MEDICAL_BILL;
            default -> OTHER;
        };
    }

    public static ProofKind parse(String name) {
        for (ProofKind kind : values()) {
            if (kind.name().equals(name)) {
                return kind;
            }
        }
        return OTHER;
    }
}
