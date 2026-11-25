package com.reimbursement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.reimbursement.exception.ClaimLimitExceededException;
import com.reimbursement.exception.DuplicateReceiptException;
import com.reimbursement.exception.InvalidClaimStateException;
import com.reimbursement.exception.MissingReceiptException;
import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.exception.UnauthorizedActionException;
import com.reimbursement.model.AuditEntry;
import com.reimbursement.model.Claim;
import com.reimbursement.model.ClaimStatus;
import com.reimbursement.model.Employee;
import com.reimbursement.model.EquipmentClaim;
import com.reimbursement.model.EventClaim;
import com.reimbursement.model.FoodClaim;
import com.reimbursement.model.Grade;
import com.reimbursement.model.ProofDocument;
import com.reimbursement.model.ProofKind;
import com.reimbursement.model.Role;
import com.reimbursement.model.TravelClaim;
import com.reimbursement.util.ProofStore;

class ClaimServiceTest {

    private final Employee rohit = new Employee(2, "Rohit Sharma", "rohit@company.com", "h", Role.MANAGER, null);
    private final Employee arjun = new Employee(4, "Arjun Singh", "arjun@company.com", "h", Role.EMPLOYEE, 2);
    private final Employee simran = new Employee(5, "Simran Kaur", "simran@company.com", "h", Role.EMPLOYEE, 2);

    @TempDir
    Path uploads;

    private FakeClaimDAO claimDAO;
    private FakeAuditDAO auditDAO;
    private FakeProofDAO proofDAO;
    private ClaimService claimService;
    private ApprovalService approvalService;

    @BeforeEach
    void setUp() {
        claimDAO = new FakeClaimDAO();
        auditDAO = new FakeAuditDAO();
        proofDAO = new FakeProofDAO();
        ProofService proofService = new ProofService(proofDAO, new ProofStore(uploads.toFile()), claimDAO);
        claimService = new ClaimService(claimDAO, auditDAO, proofService);
        approvalService = new ApprovalService(claimDAO, auditDAO, proofDAO);
    }

    @Test
    void submitSavesClaimAndWritesAudit() throws ReimbursementException {
        Claim claim = food(arjun, "300", "R1");
        submit(claim);

        assertEquals(1, claimDAO.count());
        AuditEntry entry = auditDAO.last(claim.getId());
        assertNull(entry.getFromStatus());
        assertEquals(ClaimStatus.SUBMITTED, entry.getToStatus());
    }

    @Test
    void invalidClaimIsNotSaved() {
        Claim claim = food(arjun, "3000", "R1");
        assertThrows(ClaimLimitExceededException.class, () -> submit(claim));
        assertEquals(0, claimDAO.count());
    }

    @Test
    void duplicateReceiptIsBlocked() throws ReimbursementException {
        submit(food(arjun, "300", "R1"));
        Claim copy = food(simran, "300", " R1 ");
        assertThrows(DuplicateReceiptException.class, () -> submit(copy));
    }

    @Test
    void blankReceiptIsStoredAsNull() throws ReimbursementException {
        Claim first = food(arjun, "100", "  ");
        Claim second = food(arjun, "100", "");
        submit(first);
        submit(second);

        assertNull(first.getReceiptNo());
        assertEquals(2, claimDAO.count());
    }

    @Test
    void rejectedClaimCanBeResubmitted() throws ReimbursementException {
        Claim claim = travel(arjun, "2000", "T1");
        submit(claim);
        approvalService.reject(claim, rohit, "Wrong date");

        claimService.resubmit(claim, arjun);

        assertEquals(ClaimStatus.SUBMITTED, claim.getStatus());
        assertEquals(1, claim.getResubmitCount());
        assertNull(claim.getRejectReason());
        assertEquals(ClaimStatus.REJECTED, auditDAO.last(claim.getId()).getFromStatus());
    }

    @Test
    void resubmitKeepingOwnReceiptIsNotADuplicate() throws ReimbursementException {
        Claim claim = travel(arjun, "2000", "T1");
        submit(claim);
        approvalService.reject(claim, rohit, "Amount looks high");

        claim.setAmount(new BigDecimal("1800"));
        claimService.resubmit(claim, arjun);

        assertEquals(ClaimStatus.SUBMITTED, claim.getStatus());
    }

    @Test
    void onlyRejectedClaimCanBeResubmitted() throws ReimbursementException {
        Claim claim = food(arjun, "300", "R1");
        submit(claim);
        assertThrows(InvalidClaimStateException.class, () -> claimService.resubmit(claim, arjun));
    }

    @Test
    void onlyOwnerCanResubmit() throws ReimbursementException {
        Claim claim = food(arjun, "300", "R1");
        submit(claim);
        approvalService.reject(claim, rohit, "No bill");
        assertThrows(UnauthorizedActionException.class, () -> claimService.resubmit(claim, simran));
    }

    @Test
    void ownerCanLoadOwnClaim() throws ReimbursementException {
        Claim claim = food(arjun, "300", "R1");
        submit(claim);
        assertEquals(claim, claimService.findOwnClaim(claim.getId(), arjun));
    }

    @Test
    void otherEmployeeCannotLoadTheClaim() throws ReimbursementException {
        Claim claim = food(arjun, "300", "R1");
        submit(claim);
        assertThrows(UnauthorizedActionException.class, () -> claimService.findOwnClaim(claim.getId(), simran));
    }

    @Test
    void missingClaimGivesClearError() {
        assertThrows(ReimbursementException.class, () -> claimService.findOwnClaim(99, arjun));
    }

    @Test
    void findClaimsOfReturnsOnlyThatEmployee() throws ReimbursementException {
        submit(food(arjun, "100", "A1"));
        submit(food(arjun, "200", "A2"));
        submit(food(simran, "300", "S1"));

        assertEquals(2, claimService.findClaimsOf(arjun).size());
        assertTrue(claimService.findClaimsOf(rohit).isEmpty());
    }

    @Test
    void claimThatNeedsProofWithoutDocumentIsRejected() {
        Claim claim = travel(arjun, "2000", "T1");
        assertThrows(MissingReceiptException.class, () -> claimService.submit(claim, List.of()));
        assertEquals(0, claimDAO.count());
    }

    @Test
    void approvalLetterAloneDoesNotCountAsTheReceipt() throws ReimbursementException {
        Claim claim = travel(arjun, "2000", "T1");
        ProofUpload letter = new ProofUpload(ProofKind.APPROVAL_LETTER, "letter.pdf", "application/pdf",
                pdf().getBytes());
        assertThrows(MissingReceiptException.class, () -> claimService.submit(claim, List.of(letter)));
        assertEquals(0, claimDAO.count());

        claimService.submit(claim, List.of(pdf()));
        assertEquals(1, proofDAO.findByClaim(claim.getId()).size());
    }

    @Test
    void fakePdfIsRejected() {
        ProofUpload fake = new ProofUpload(ProofKind.TICKET, "ticket.pdf", "application/pdf",
                "MZ this is really a program".getBytes());
        assertThrows(ReimbursementException.class,
                () -> claimService.submit(travel(arjun, "2000", "T1"), List.of(fake)));
    }

    @Test
    void wrongFileTypeIsRejected() {
        ProofUpload text = new ProofUpload(ProofKind.TICKET, "notes.txt", "text/plain", "hello there friend".getBytes());
        assertThrows(ReimbursementException.class,
                () -> claimService.submit(travel(arjun, "2000", "T1"), List.of(text)));
    }

    @Test
    void equipmentIsCompanyOnlyAndNeedsAnInvoiceFile() throws ReimbursementException {
        Employee student = new Employee(8, "Riya Sharma", "riya@thapar.edu", "h", Role.EMPLOYEE, 11, Grade.STUDENT);
        Claim studentEquipment = new EquipmentClaim(student, new BigDecimal("3000"), LocalDate.now(), "R1", "Monitor");
        Claim companyEquipment = new EquipmentClaim(arjun, new BigDecimal("3000"), LocalDate.now(), "R2", "Monitor");
        Claim companyEvent = new EventClaim(arjun, new BigDecimal("3000"), LocalDate.now(), "R3", "Academic event");
        ProofUpload invoice = new ProofUpload(ProofKind.EQUIPMENT_INVOICE, "invoice.pdf", "application/pdf",
                pdf().getBytes());

        assertThrows(ReimbursementException.class, () -> claimService.submit(studentEquipment, List.of(invoice)));
        assertThrows(ReimbursementException.class, () -> claimService.submit(companyEvent, List.of(pdf())));
        assertThrows(MissingReceiptException.class, () -> claimService.submit(companyEquipment));
        assertEquals(0, claimDAO.count());

        claimService.submit(companyEquipment, List.of(invoice));
        assertEquals(ProofKind.EQUIPMENT_INVOICE, proofDAO.findByClaim(companyEquipment.getId()).getFirst().getKind());
    }

    @Test
    void companyCanResubmitALegacyEventClaim() throws ReimbursementException {
        Claim event = new EventClaim(arjun, new BigDecimal("250"), LocalDate.now(), "R1", "Legacy claim");
        event.setStatus(ClaimStatus.REJECTED);
        claimDAO.save(event);

        claimService.resubmit(event, arjun, List.of(pdf()));
        assertEquals(ClaimStatus.SUBMITTED, event.getStatus());
    }

    @Test
    void atMostTwoDocuments() {
        assertThrows(ReimbursementException.class,
                () -> claimService.submit(travel(arjun, "2000", "T1"), List.of(pdf(), pdf(), pdf())));
    }

    @Test
    void documentIsSavedOnDiskAndInTheDatabase() throws ReimbursementException {
        Claim claim = travel(arjun, "2000", "T1");
        claimService.submit(claim, List.of(pdf()));

        List<ProofDocument> proofs = proofDAO.findByClaim(claim.getId());
        assertEquals(1, proofs.size());
        assertEquals("bill.pdf", proofs.get(0).getOriginalName());
        assertTrue(uploads.resolve(proofs.get(0).getStoredName()).toFile().exists());
    }

    @Test
    void resubmitNeedsTheManagerToCheckProofAgain() throws ReimbursementException {
        Claim claim = travel(arjun, "2000", "T1");
        claimService.submit(claim, List.of(pdf()));
        claim.setProofChecked(true);
        approvalService.reject(claim, rohit, "Wrong date");

        claimService.resubmit(claim, arjun);
        assertFalse(claim.isProofChecked());
    }

    // Attaches a test PDF whenever the claim type needs proof
    private void submit(Claim claim) throws ReimbursementException {
        claimService.submit(claim, claim.needsProof() ? List.of(pdf()) : List.of());
    }

    static ProofUpload pdf() {
        return new ProofUpload(ProofKind.OTHER, "bill.pdf", "application/pdf", "%PDF-1.4 test file".getBytes());
    }

    private Claim food(Employee owner, String amount, String receipt) {
        return new FoodClaim(owner, new BigDecimal(amount), LocalDate.now(), receipt, "Meal");
    }

    private Claim travel(Employee owner, String amount, String receipt) {
        return new TravelClaim(owner, new BigDecimal(amount), LocalDate.now(), receipt, "Cab");
    }
}
