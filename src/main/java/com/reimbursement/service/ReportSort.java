package com.reimbursement.service;

import java.util.Comparator;

import com.reimbursement.model.Claim;

public enum ReportSort {
    NEWEST_FIRST("Newest expense first", Comparator.comparing(Claim::getExpenseDate).reversed()),
    OLDEST_FIRST("Oldest expense first", Comparator.comparing(Claim::getExpenseDate)),
    AMOUNT_HIGH_FIRST("Highest amount first", Comparator.comparing(Claim::getAmount).reversed()),
    AMOUNT_LOW_FIRST("Lowest amount first", Comparator.comparing(Claim::getAmount)),
    STATUS("By status", Comparator.comparing(Claim::getStatus).thenComparing(Claim::getId));

    private final String label;
    private final Comparator<Claim> comparator;

    ReportSort(String label, Comparator<Claim> comparator) {
        this.label = label;
        this.comparator = comparator;
    }

    public static ReportSort parse(String name) {
        for (ReportSort sort : values()) {
            if (sort.name().equals(name)) {
                return sort;
            }
        }
        return NEWEST_FIRST;
    }

    public Comparator<Claim> getComparator() {
        return comparator;
    }

    @Override
    public String toString() {
        return label;
    }
}
