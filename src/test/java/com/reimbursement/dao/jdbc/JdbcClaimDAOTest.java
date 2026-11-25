package com.reimbursement.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.reimbursement.dao.AuditDAO;
import com.reimbursement.dao.ClaimDAO;
import com.reimbursement.dao.DBConnection;
import com.reimbursement.dao.EmployeeDAO;
import com.reimbursement.exception.DataAccessException;
import com.reimbursement.model.AuditEntry;
import com.reimbursement.model.Claim;
import com.reimbursement.model.ClaimStatus;
import com.reimbursement.model.Employee;
import com.reimbursement.model.EquipmentClaim;
import com.reimbursement.model.FoodClaim;
import com.reimbursement.model.TravelClaim;

class JdbcClaimDAOTest {

    private ClaimDAO claimDAO;
    private AuditDAO auditDAO;
    private Employee arjun;
    private final List<Integer> createdIds = new ArrayList<>();

    @BeforeEach
    void setUp() throws DataAccessException {
        assumeTrue(new File("db.properties").exists(), "db.properties not found");
        EmployeeDAO employeeDAO = new JdbcEmployeeDAO();
        claimDAO = new JdbcClaimDAO(employeeDAO);
        auditDAO = new JdbcAuditDAO();
        arjun = employeeDAO.findById(4).orElseThrow();
    }

    // Remove test rows so the seed data stays clean
    @AfterEach
    void cleanUp() throws SQLException {
        try (Connection con = DBConnection.getConnection();
                PreparedStatement audit = con.prepareStatement("DELETE FROM claim_audit WHERE claim_id = ?");
                PreparedStatement claims = con.prepareStatement("DELETE FROM claims WHERE id = ?")) {
            for (int id : createdIds) {
                audit.setInt(1, id);
                audit.executeUpdate();
                claims.setInt(1, id);
                claims.executeUpdate();
            }
        }
    }

    @Test
    void savedClaimLoadsBackWithSameTypeAndFields() throws DataAccessException {
        Claim claim = new TravelClaim(arjun, new BigDecimal("1500.50"), LocalDate.now().minusDays(3),
                uniqueReceipt(), "Train ticket");
        save(claim);

        Claim loaded = claimDAO.findById(claim.getId()).orElseThrow();
        assertInstanceOf(TravelClaim.class, loaded);
        assertEquals(0, new BigDecimal("1500.50").compareTo(loaded.getAmount()));
        assertEquals(claim.getExpenseDate(), loaded.getExpenseDate());
        assertEquals(ClaimStatus.SUBMITTED, loaded.getStatus());
        assertEquals("Arjun Singh", loaded.getEmployee().getName());
    }

    @Test
    void claimWithoutReceiptStoresNull() throws DataAccessException {
        Claim claim = new FoodClaim(arjun, new BigDecimal("200"), LocalDate.now(), null, "Tea");
        save(claim);

        assertNull(claimDAO.findById(claim.getId()).orElseThrow().getReceiptNo());
    }

    @Test
    void equipmentClaimRoundTripsWithInvoiceNumber() throws DataAccessException {
        Claim equipment = new EquipmentClaim(arjun, new BigDecimal("12000"), LocalDate.now(),
                uniqueReceipt(), "Desktop monitor");
        save(equipment);

        Claim loaded = claimDAO.findById(equipment.getId()).orElseThrow();
        assertInstanceOf(EquipmentClaim.class, loaded);
        assertEquals(equipment.getReceiptNo(), loaded.getReceiptNo());
        assertEquals("Desktop monitor", loaded.getDescription());
    }

    @Test
    void updateChangesStatusAndReason() throws DataAccessException {
        Claim claim = new FoodClaim(arjun, new BigDecimal("300"), LocalDate.now(), null, "Snacks");
        save(claim);

        claim.setStatus(ClaimStatus.REJECTED);
        claim.setRejectReason("No bill attached");
        claimDAO.update(claim);

        Claim loaded = claimDAO.findById(claim.getId()).orElseThrow();
        assertEquals(ClaimStatus.REJECTED, loaded.getStatus());
        assertEquals("No bill attached", loaded.getRejectReason());
    }

    @Test
    void receiptCheckIgnoresTheClaimItself() throws DataAccessException {
        String receipt = uniqueReceipt();
        Claim claim = new TravelClaim(arjun, new BigDecimal("2000"), LocalDate.now(), receipt, "Flight");
        save(claim);

        assertTrue(claimDAO.isReceiptUsed(receipt, 0, false));
        assertFalse(claimDAO.isReceiptUsed(receipt, claim.getId(), false));
        assertFalse(claimDAO.isReceiptUsed(receipt, 0, true));
    }

    @Test
    void auditEntriesComeBackInOrder() throws DataAccessException {
        Claim claim = new FoodClaim(arjun, new BigDecimal("150"), LocalDate.now(), null, "Coffee");
        save(claim);

        auditDAO.save(new AuditEntry(claim.getId(), 4, null, ClaimStatus.SUBMITTED, "Submitted"));
        auditDAO.save(new AuditEntry(claim.getId(), 2, ClaimStatus.SUBMITTED, ClaimStatus.APPROVED, "Looks fine"));

        List<AuditEntry> entries = auditDAO.findByClaim(claim.getId());
        assertEquals(2, entries.size());
        assertNull(entries.get(0).getFromStatus());
        assertEquals(ClaimStatus.APPROVED, entries.get(1).getToStatus());
    }

    private void save(Claim claim) throws DataAccessException {
        claimDAO.save(claim);
        createdIds.add(claim.getId());
    }

    private String uniqueReceipt() {
        return "TEST-" + System.nanoTime();
    }
}
