package com.reimbursement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.reimbursement.exception.DataAccessException;
import com.reimbursement.model.Claim;
import com.reimbursement.model.ClaimStatus;
import com.reimbursement.model.Employee;
import com.reimbursement.model.FoodClaim;
import com.reimbursement.model.Role;
import com.reimbursement.model.TravelClaim;
import com.reimbursement.util.ReportWriter;

class ReportServiceTest {

    private final Employee neha = new Employee(1, "Neha Kapoor", "neha@company.com", "h", Role.FINANCE, null);
    private final Employee rohit = new Employee(2, "Rohit Sharma", "rohit@company.com", "h", Role.MANAGER, null);
    private final Employee arjun = new Employee(4, "Arjun Singh", "arjun@company.com", "h", Role.EMPLOYEE, 2);
    private final Employee simran = new Employee(5, "Simran Kaur", "simran@company.com", "h", Role.EMPLOYEE, 2);
    private final Employee karan = new Employee(6, "Karan Verma", "karan@company.com", "h", Role.EMPLOYEE, 3);

    private ReportService reportService;
    private Claim small;
    private Claim big;
    private Claim other;

    @BeforeEach
    void setUp() {
        FakeClaimDAO claimDAO = new FakeClaimDAO();
        reportService = new ReportService(claimDAO, new ReportWriter());

        small = new FoodClaim(arjun, new BigDecimal("300"), LocalDate.now().minusDays(10), null, "Lunch");
        big = new TravelClaim(simran, new BigDecimal("9000"), LocalDate.now().minusDays(2), "T1", "Flight");
        other = new FoodClaim(karan, new BigDecimal("1200"), LocalDate.now().minusDays(5), "F1", "Dinner");
        big.setStatus(ClaimStatus.APPROVED);
        other.setStatus(ClaimStatus.REIMBURSED);

        claimDAO.save(small);
        claimDAO.save(big);
        claimDAO.save(other);
    }

    @Test
    void financeSeesAllClaims() throws DataAccessException {
        assertEquals(3, reportService.findClaimsFor(neha).size());
    }

    @Test
    void managerSeesOnlyTheirTeam() throws DataAccessException {
        List<Claim> claims = reportService.findClaimsFor(rohit);
        assertEquals(2, claims.size());
        assertTrue(claims.contains(small));
        assertTrue(claims.contains(big));
    }

    @Test
    void employeeSeesOnlyOwnClaims() throws DataAccessException {
        assertEquals(List.of(small), reportService.findClaimsFor(arjun));
    }

    @Test
    void sortByAmountHighestFirst() {
        List<Claim> sorted = reportService.sort(List.of(small, big, other), ReportSort.AMOUNT_HIGH_FIRST);
        assertEquals(List.of(big, other, small), sorted);
    }

    @Test
    void sortByDateNewestFirst() {
        List<Claim> sorted = reportService.sort(List.of(small, big, other), ReportSort.NEWEST_FIRST);
        assertEquals(List.of(big, other, small), sorted);
    }

    @Test
    void sortByStatusFollowsLifecycleOrder() {
        List<Claim> sorted = reportService.sort(List.of(other, big, small), ReportSort.STATUS);
        assertEquals(List.of(small, big, other), sorted);
    }

    @Test
    void sortNameFromUrlIsParsed() {
        assertEquals(ReportSort.AMOUNT_HIGH_FIRST, ReportSort.parse("AMOUNT_HIGH_FIRST"));
    }

    @Test
    void unknownOrMissingSortFallsBackToNewest() {
        assertEquals(ReportSort.NEWEST_FIRST, ReportSort.parse("DROP TABLE"));
        assertEquals(ReportSort.NEWEST_FIRST, ReportSort.parse(null));
    }

    @Test
    void countsIncludeEveryStatus() {
        Map<ClaimStatus, Integer> counts = reportService.countByStatus(List.of(small, big, other));
        assertEquals(4, counts.size());
        assertEquals(1, counts.get(ClaimStatus.SUBMITTED));
        assertEquals(0, counts.get(ClaimStatus.REJECTED));
    }

    @Test
    void totalsAddUpPerStatus() {
        Map<ClaimStatus, BigDecimal> totals = reportService.totalByStatus(List.of(small, big, other));
        assertEquals(0, new BigDecimal("9000").compareTo(totals.get(ClaimStatus.APPROVED)));
        assertEquals(0, BigDecimal.ZERO.compareTo(totals.get(ClaimStatus.REJECTED)));
    }

    @Test
    void exportWritesSummaryFile(@TempDir Path folder) throws DataAccessException, IOException {
        File file = reportService.export(neha, ReportSort.AMOUNT_HIGH_FIRST, folder.toFile());

        String text = Files.readString(file.toPath());
        assertTrue(text.startsWith("Expense Claim Summary"));
        assertTrue(text.contains("Sorted by: Highest amount first"));
        assertTrue(text.contains("10500.00"));
        assertTrue(text.indexOf("Simran Kaur") < text.indexOf("Arjun Singh"));
    }
}
