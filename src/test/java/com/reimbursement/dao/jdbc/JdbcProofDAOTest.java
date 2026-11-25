package com.reimbursement.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.reimbursement.dao.DBConnection;
import com.reimbursement.model.Claim;
import com.reimbursement.model.Employee;
import com.reimbursement.model.FoodClaim;
import com.reimbursement.model.ProofDocument;
import com.reimbursement.model.ProofKind;
import com.reimbursement.util.ProofStore;

class JdbcProofDAOTest {

    @TempDir
    Path folder;

    private final JdbcProofDAO proofs = new JdbcProofDAO();
    private int claimId;

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(Files.isRegularFile(Path.of("db.properties")), "db.properties not found");
        JdbcEmployeeDAO employees = new JdbcEmployeeDAO();
        Employee arjun = employees.findByEmail("arjun@company.com").orElseThrow();
        Claim claim = new FoodClaim(arjun, new BigDecimal("100"), LocalDate.now(), null, "Proof test");
        new JdbcClaimDAO(employees).save(claim);
        claimId = claim.getId();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        if (claimId == 0) {
            return;
        }
        try (Connection con = DBConnection.getConnection();
                PreparedStatement docs = con.prepareStatement("DELETE FROM claim_proofs WHERE claim_id = ?");
                PreparedStatement claim = con.prepareStatement("DELETE FROM claims WHERE id = ?")) {
            docs.setInt(1, claimId);
            docs.executeUpdate();
            claim.setInt(1, claimId);
            claim.executeUpdate();
        }
    }

    @Test
    void findsSavedDocumentsByIdAndClaim() throws Exception {
        assertTrue(proofs.findByClaim(claimId).isEmpty());
        ProofDocument bill = save(ProofKind.FOOD_BILL, "bill.pdf");
        ProofDocument letter = save(ProofKind.APPROVAL_LETTER, "letter.pdf");

        assertTrue(bill.getId() > 0);
        ProofDocument found = proofs.findById(bill.getId()).orElseThrow();
        assertEquals(claimId, found.getClaimId());
        assertEquals(ProofKind.FOOD_BILL, found.getKind());
        assertEquals("bill.pdf", found.getOriginalName());
        assertEquals(bill.getStoredName(), found.getStoredName());
        assertEquals("application/pdf", found.getContentType());
        assertEquals(bill.getSizeBytes(), found.getSizeBytes());
        assertTrue(Files.isRegularFile(folder.resolve(found.getStoredName())));

        List<ProofDocument> documents = proofs.findByClaim(claimId);
        assertEquals(2, documents.size());
        assertEquals(bill.getId(), documents.get(0).getId());
        assertEquals(letter.getId(), documents.get(1).getId());
        assertTrue(proofs.findById(0).isEmpty());
    }

    private ProofDocument save(ProofKind kind, String name) throws Exception {
        byte[] bytes = "%PDF-1.4 test document".getBytes(StandardCharsets.US_ASCII);
        String storedName = new ProofStore(folder.toFile()).save(bytes, "application/pdf");
        ProofDocument proof = new ProofDocument(claimId, kind, name, storedName, "application/pdf", bytes.length);
        proofs.save(proof);
        return proof;
    }
}
