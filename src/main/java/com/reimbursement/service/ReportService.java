package com.reimbursement.service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.reimbursement.dao.ClaimDAO;
import com.reimbursement.exception.DataAccessException;
import com.reimbursement.model.Claim;
import com.reimbursement.model.ClaimStatus;
import com.reimbursement.model.Employee;
import com.reimbursement.model.Role;
import com.reimbursement.util.ReportWriter;

public class ReportService {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final ClaimDAO claimDAO;
    private final ReportWriter reportWriter;

    public ReportService(ClaimDAO claimDAO, ReportWriter reportWriter) {
        this.claimDAO = claimDAO;
        this.reportWriter = reportWriter;
    }

    public List<Claim> findClaimsFor(Employee viewer) throws DataAccessException {
        if (viewer.getRole() == Role.EMPLOYEE) {
            return claimDAO.findByEmployee(viewer.getId());
        }
        List<Claim> team = new ArrayList<>();
        for (Claim claim : claimDAO.findAll()) {
            Employee owner = claim.getEmployee();
            Integer managerId = owner.getManagerId();
            if (owner.isUniversity() == viewer.isUniversity()
                    && (viewer.getRole() == Role.FINANCE || (managerId != null && managerId == viewer.getId()))) {
                team.add(claim);
            }
        }
        return team;
    }

    public List<Claim> sort(List<Claim> claims, ReportSort sort) {
        List<Claim> sorted = new ArrayList<>(claims);
        sorted.sort(sort.getComparator());
        return sorted;
    }

    public Map<ClaimStatus, Integer> countByStatus(List<Claim> claims) {
        Map<ClaimStatus, Integer> counts = new EnumMap<>(ClaimStatus.class);
        for (ClaimStatus status : ClaimStatus.values()) {
            counts.put(status, 0);
        }
        for (Claim claim : claims) {
            counts.put(claim.getStatus(), counts.get(claim.getStatus()) + 1);
        }
        return counts;
    }

    public Map<ClaimStatus, BigDecimal> totalByStatus(List<Claim> claims) {
        Map<ClaimStatus, BigDecimal> totals = new EnumMap<>(ClaimStatus.class);
        for (ClaimStatus status : ClaimStatus.values()) {
            totals.put(status, BigDecimal.ZERO);
        }
        for (Claim claim : claims) {
            totals.put(claim.getStatus(), totals.get(claim.getStatus()).add(claim.getAmount()));
        }
        return totals;
    }

    public BigDecimal totalOf(List<Claim> claims) {
        BigDecimal total = BigDecimal.ZERO;
        for (Claim claim : claims) {
            total = total.add(claim.getAmount());
        }
        return total;
    }

    // Share of the overall amount for each status, as a whole percent, for the bar chart
    public Map<ClaimStatus, Integer> percentByStatus(Map<ClaimStatus, BigDecimal> totals) {
        BigDecimal all = BigDecimal.ZERO;
        for (BigDecimal amount : totals.values()) {
            all = all.add(amount);
        }
        Map<ClaimStatus, Integer> percents = new EnumMap<>(ClaimStatus.class);
        for (Map.Entry<ClaimStatus, BigDecimal> entry : totals.entrySet()) {
            int percent = 0;
            if (all.signum() > 0) {
                percent = entry.getValue().multiply(BigDecimal.valueOf(100)).divide(all, 0, RoundingMode.HALF_UP).intValue();
            }
            percents.put(entry.getKey(), percent);
        }
        return percents;
    }

    public File export(Employee viewer, ReportSort sort, File folder) throws DataAccessException, IOException {
        List<Claim> claims = sort(findClaimsFor(viewer), sort);
        java.nio.file.Files.createDirectories(folder.toPath());
        String stamp = LocalDateTime.now().format(FILE_STAMP);
        File file = new File(folder, "claim-summary-" + stamp + "-" + UUID.randomUUID() + ".txt");
        reportWriter.write(file, viewer, sort.toString(), claims, countByStatus(claims), totalByStatus(claims));
        return file;
    }
}
