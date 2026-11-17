package com.reimbursement.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import com.reimbursement.model.Claim;
import com.reimbursement.model.ClaimStatus;
import com.reimbursement.model.Employee;

public class ReportWriter {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final String NEW_LINE = System.lineSeparator();

    public void write(File file, Employee viewer, String sortLabel, List<Claim> claims,
            Map<ClaimStatus, Integer> counts, Map<ClaimStatus, BigDecimal> totals) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Expense Claim Summary" + NEW_LINE);
            writer.write("Generated on " + LocalDateTime.now().format(STAMP) + " by " + viewer.getName()
                    + " (" + viewer.getRole() + ")" + NEW_LINE);
            writer.write("Sorted by: " + sortLabel + NEW_LINE + NEW_LINE);

            writer.write(String.format("%-12s %6s %14s%n", "Status", "Count", "Total (Rs)"));
            int allCount = 0;
            BigDecimal allTotal = BigDecimal.ZERO;
            for (ClaimStatus status : counts.keySet()) {
                writer.write(String.format("%-12s %6d %14s%n", status, counts.get(status), money(totals.get(status))));
                allCount += counts.get(status);
                allTotal = allTotal.add(totals.get(status));
            }
            writer.write(String.format("%-12s %6d %14s%n%n", "All", allCount, money(allTotal)));

            writer.write(String.format("%-5s %-16s %-8s %12s %-12s %-11s%n",
                    "ID", "Employee", "Type", "Amount", "Expense date", "Status"));
            for (Claim claim : claims) {
                writer.write(String.format("%-5d %-16s %-8s %12s %-12s %-11s%n",
                        claim.getId(), claim.getEmployee().getName(), claim.getType(),
                        money(claim.getAmount()), claim.getExpenseDate(), claim.getStatus()));
            }
        }
    }

    private String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
