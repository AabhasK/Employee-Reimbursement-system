package com.reimbursement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.reimbursement.exception.InvalidClaimStateException;
import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.exception.SelfApprovalException;
import com.reimbursement.exception.UnauthorizedActionException;
import com.reimbursement.model.Claim;
import com.reimbursement.model.ClaimStatus;
import com.reimbursement.model.Employee;
import com.reimbursement.model.MedicalClaim;
import com.reimbursement.model.Role;
import com.reimbursement.util.ProofStore;
import com.reimbursement.util.ReportWriter;

class ApprovalServiceTest {

    private final Employee neha = new Employee(1, "Neha Kapoor", "neha@company.com", "h", Role.FINANCE, null);
    private final Employee rohit = new Employee(2, "Rohit Sharma", "rohit@company.com", "h", Role.MANAGER, null);
    private final Employee priya = new Employee(3, "Priya Mehta", "priya@company.com", "h", Role.MANAGER, null);
    private final Employee arjun = new Employee(4, "Arjun Singh", "arjun@company.com", "h", Role.EMPLOYEE, 2);
    private final Employee simran = new Employee(5, "Simran Kaur", "simran@company.com", "h", Role.EMPLOYEE, 2);

    @TempDir
    Path uploads;

    private FakeAuditDAO auditDAO;
    private FakeClaimDAO claimDAO;
    private FakeProofDAO proofDAO;
    private ClaimService claimService;
    private ApprovalService approvalService;
    private int receiptCounter = 0;

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
    void managerAloneApprovesClaimAtThreshold() throws ReimbursementException {
        Claim claim = submit(arjun, "5000");
        approvalService.approve(claim, rohit, "ok", true);
        assertEquals(ClaimStatus.APPROVED, claim.getStatus());
    }

    @Test
    void claimAboveThresholdWaitsForFinance() throws ReimbursementException {
        Claim claim = submit(arjun, "5000.01");

        approvalService.approve(claim, rohit, "ok from manager", true);
        assertEquals(ClaimStatus.SUBMITTED, claim.getStatus());

        approvalService.approve(claim, neha, "ok from finance");
        assertEquals(ClaimStatus.APPROVED, claim.getStatus());
    }

    @Test
    void financeCannotApproveBeforeManager() throws ReimbursementException {
        Claim claim = submit(arjun, "8000");
        assertThrows(InvalidClaimStateException.class, () -> approvalService.approve(claim, neha, "ok"));
    }

    @Test
    void universityFinanceCannotSeeOrActOnCompanyClaims() throws ReimbursementException {
        Employee student = new Employee(8, "Riya", "riya@thapar.edu", "h", Role.EMPLOYEE, 11);
        Employee reviewer = new Employee(11, "Reviewer", "reviewer@thapar.edu", "h", Role.MANAGER, null);
        Employee finance = new Employee(12, "Finance", "finance@thapar.edu", "h", Role.FINANCE, null);
        Claim company = submit(arjun, "8000");
        Claim university = submit(student, "8000");
        approvalService.approve(company, rohit, "ok", true);
        approvalService.approve(university, reviewer, "ok", true);

        assertEquals(List.of(company), approvalService.findPendingFor(neha));
        assertEquals(List.of(university), approvalService.findPendingFor(finance));
        assertThrows(UnauthorizedActionException.class, () -> approvalService.findClaim(company.getId(), finance));
        assertThrows(UnauthorizedActionException.class, () -> approvalService.approve(company, finance, "ok"));
        assertThrows(UnauthorizedActionException.class, () -> approvalService.reject(university, neha, "reason"));
        approvalService.approve(company, neha, "ok");
        approvalService.approve(university, finance, "ok");
        assertEquals(List.of(university), approvalService.findReadyToReimburse(finance));
        assertThrows(UnauthorizedActionException.class, () -> approvalService.markReimbursed(company, finance));

        ReportService reports = new ReportService(claimDAO, new ReportWriter());
        assertEquals(List.of(university), reports.findClaimsFor(finance));
        ProofService proofs = new ProofService(proofDAO, new ProofStore(uploads.toFile()), claimDAO);
        int companyProof = proofDAO.findByClaim(company.getId()).getFirst().getId();
        assertThrows(UnauthorizedActionException.class, () -> proofs.findForViewer(companyProof, finance));
        assertEquals(companyProof, proofs.findForViewer(companyProof, neha).getId());
    }

    @Test
    void financeDoesNotApproveSmallClaims() throws ReimbursementException {
        Claim claim = submit(arjun, "800");
        assertThrows(UnauthorizedActionException.class, () -> approvalService.approve(claim, neha, "ok"));
    }

    @Test
    void managerCannotApproveTwice() throws ReimbursementException {
        Claim claim = submit(arjun, "8000");
        approvalService.approve(claim, rohit, "ok", true);
        assertThrows(InvalidClaimStateException.class, () -> approvalService.approve(claim, rohit, "again", true));
    }

    @Test
    void ownClaimCannotBeApproved() throws ReimbursementException {
        Claim claim = submit(neha, "8000");
        assertThrows(SelfApprovalException.class, () -> approvalService.approve(claim, neha, "mine"));
    }

    @Test
    void otherManagerCannotAct() throws ReimbursementException {
        Claim claim = submit(arjun, "800");
        assertThrows(UnauthorizedActionException.class, () -> approvalService.approve(claim, priya, "ok"));
    }

    @Test
    void employeeCannotApprove() throws ReimbursementException {
        Claim claim = submit(arjun, "800");
        assertThrows(UnauthorizedActionException.class, () -> approvalService.approve(claim, simran, "ok"));
    }

    @Test
    void rejectNeedsAReason() throws ReimbursementException {
        Claim claim = submit(arjun, "800");
        assertThrows(ReimbursementException.class, () -> approvalService.reject(claim, rohit, " "));
    }

    @Test
    void tooLongReasonIsRejected() throws ReimbursementException {
        Claim claim = submit(arjun, "800");
        assertThrows(ReimbursementException.class, () -> approvalService.reject(claim, rohit, "x".repeat(256)));
        assertEquals(ClaimStatus.SUBMITTED, claim.getStatus());
    }

    @Test
    void rejectStoresReason() throws ReimbursementException {
        Claim claim = submit(arjun, "800");
        approvalService.reject(claim, rohit, "Bill is unclear");

        assertEquals(ClaimStatus.REJECTED, claim.getStatus());
        assertEquals("Bill is unclear", claim.getRejectReason());
    }

    @Test
    void approvedClaimCannotBeRejected() throws ReimbursementException {
        Claim claim = submit(arjun, "800");
        approvalService.approve(claim, rohit, "ok", true);
        assertThrows(InvalidClaimStateException.class, () -> approvalService.reject(claim, rohit, "changed mind"));
    }

    @Test
    void financeReimbursesApprovedClaim() throws ReimbursementException {
        Claim claim = submit(arjun, "800");
        approvalService.approve(claim, rohit, "ok", true);
        approvalService.markReimbursed(claim, neha);

        assertEquals(ClaimStatus.REIMBURSED, claim.getStatus());
        assertThrows(InvalidClaimStateException.class, () -> approvalService.markReimbursed(claim, neha));
    }

    @Test
    void submittedClaimCannotBeReimbursed() throws ReimbursementException {
        Claim claim = submit(arjun, "800");
        assertThrows(InvalidClaimStateException.class, () -> approvalService.markReimbursed(claim, neha));
    }

    @Test
    void managerCannotReimburse() throws ReimbursementException {
        Claim claim = submit(arjun, "800");
        approvalService.approve(claim, rohit, "ok", true);
        assertThrows(UnauthorizedActionException.class, () -> approvalService.markReimbursed(claim, rohit));
    }

    @Test
    void managerMustTickProofBeforeApproving() throws ReimbursementException {
        Claim claim = submit(arjun, "800");
        assertThrows(ReimbursementException.class, () -> approvalService.approve(claim, rohit, "ok"));
        assertEquals(ClaimStatus.SUBMITTED, claim.getStatus());
        assertFalse(claim.isProofChecked());
    }

    @Test
    void proofTickIsSavedOnTheClaim() throws ReimbursementException {
        Claim claim = submit(arjun, "9000");
        approvalService.approve(claim, rohit, "Ticket matches", true);
        assertTrue(claim.isProofChecked());

        approvalService.approve(claim, neha, "ok");
        assertEquals(ClaimStatus.APPROVED, claim.getStatus());
    }

    @Test
    void onlyOwnerManagerAndFinanceCanOpenDocuments() throws ReimbursementException {
        Claim claim = submit(arjun, "800");
        ProofService proofService = new ProofService(proofDAO, new ProofStore(uploads.toFile()), claimDAO);
        int proofId = proofDAO.findByClaim(claim.getId()).get(0).getId();

        assertEquals(proofId, proofService.findForViewer(proofId, arjun).getId());
        assertEquals(proofId, proofService.findForViewer(proofId, rohit).getId());
        assertEquals(proofId, proofService.findForViewer(proofId, neha).getId());
        assertThrows(UnauthorizedActionException.class, () -> proofService.findForViewer(proofId, priya));
        assertThrows(UnauthorizedActionException.class, () -> proofService.findForViewer(proofId, simran));
    }

    @Test
    void onlyManagerApprovedBigClaimsWaitForFinance() throws ReimbursementException {
        Claim small = submit(arjun, "800");
        Claim big = submit(arjun, "9000");
        Claim untouched = submit(arjun, "7000");
        approvalService.approve(small, rohit, "ok", true);
        approvalService.approve(big, rohit, "ok", true);

        assertEquals(Set.of(big.getId()), approvalService.findWaitingForFinance(List.of(small, big, untouched)));
    }

    @Test
    void findClaimLoadsById() throws ReimbursementException {
        Claim claim = submit(arjun, "800");
        assertEquals(claim, approvalService.findClaim(claim.getId(), rohit));
    }

    @Test
    void findClaimWithUnknownIdGivesClearError() {
        assertThrows(ReimbursementException.class, () -> approvalService.findClaim(999, rohit));
    }

    @Test
    void pendingListsFollowEscalation() throws ReimbursementException {
        Claim small = submit(arjun, "800");
        Claim big = submit(arjun, "9000");

        assertEquals(2, approvalService.findPendingFor(rohit).size());
        assertTrue(approvalService.findPendingFor(priya).isEmpty());
        assertTrue(approvalService.findPendingFor(neha).isEmpty());

        approvalService.approve(small, rohit, "ok", true);
        approvalService.approve(big, rohit, "ok", true);

        assertTrue(approvalService.findPendingFor(rohit).isEmpty());
        assertEquals(1, approvalService.findPendingFor(neha).size());
        assertEquals(big, approvalService.findPendingFor(neha).get(0));
    }

    @Test
    void resubmittedClaimNeedsManagerAgain() throws ReimbursementException {
        Claim claim = submit(arjun, "9000");
        approvalService.approve(claim, rohit, "ok", true);
        approvalService.reject(claim, neha, "Missing hotel bill");
        claimService.resubmit(claim, arjun);

        assertThrows(InvalidClaimStateException.class, () -> approvalService.approve(claim, neha, "ok"));
    }

    private Claim submit(Employee owner, String amount) throws ReimbursementException {
        receiptCounter++;
        Claim claim = new MedicalClaim(owner, new BigDecimal(amount), LocalDate.now(), "M" + receiptCounter, "Clinic");
        claimService.submit(claim, List.of(ClaimServiceTest.pdf()));
        return claim;
    }
}
