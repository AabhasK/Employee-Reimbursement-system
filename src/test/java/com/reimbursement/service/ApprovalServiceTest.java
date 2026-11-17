package com.reimbursement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.reimbursement.exception.InvalidClaimStateException;
import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.exception.SelfApprovalException;
import com.reimbursement.exception.UnauthorizedActionException;
import com.reimbursement.model.Claim;
import com.reimbursement.model.ClaimStatus;
import com.reimbursement.model.Employee;
import com.reimbursement.model.MedicalClaim;
import com.reimbursement.model.Role;

class ApprovalServiceTest {

    private final Employee neha = new Employee(1, "Neha Kapoor", "neha@company.com", "h", Role.FINANCE, null);
    private final Employee rohit = new Employee(2, "Rohit Sharma", "rohit@company.com", "h", Role.MANAGER, null);
    private final Employee priya = new Employee(3, "Priya Mehta", "priya@company.com", "h", Role.MANAGER, null);
    private final Employee arjun = new Employee(4, "Arjun Singh", "arjun@company.com", "h", Role.EMPLOYEE, 2);
    private final Employee simran = new Employee(5, "Simran Kaur", "simran@company.com", "h", Role.EMPLOYEE, 2);

    private FakeAuditDAO auditDAO;
    private ClaimService claimService;
    private ApprovalService approvalService;
    private int receiptCounter = 0;

    @BeforeEach
    void setUp() {
        FakeClaimDAO claimDAO = new FakeClaimDAO();
        auditDAO = new FakeAuditDAO();
        claimService = new ClaimService(claimDAO, auditDAO);
        approvalService = new ApprovalService(claimDAO, auditDAO);
    }

    @Test
    void managerAloneApprovesClaimAtThreshold() throws ReimbursementException {
        Claim claim = submit(arjun, "5000");
        approvalService.approve(claim, rohit, "ok");
        assertEquals(ClaimStatus.APPROVED, claim.getStatus());
    }

    @Test
    void claimAboveThresholdWaitsForFinance() throws ReimbursementException {
        Claim claim = submit(arjun, "5000.01");

        approvalService.approve(claim, rohit, "ok from manager");
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
    void financeDoesNotApproveSmallClaims() throws ReimbursementException {
        Claim claim = submit(arjun, "800");
        assertThrows(UnauthorizedActionException.class, () -> approvalService.approve(claim, neha, "ok"));
    }

    @Test
    void managerCannotApproveTwice() throws ReimbursementException {
        Claim claim = submit(arjun, "8000");
        approvalService.approve(claim, rohit, "ok");
        assertThrows(InvalidClaimStateException.class, () -> approvalService.approve(claim, rohit, "again"));
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
        approvalService.approve(claim, rohit, "ok");
        assertThrows(InvalidClaimStateException.class, () -> approvalService.reject(claim, rohit, "changed mind"));
    }

    @Test
    void financeReimbursesApprovedClaim() throws ReimbursementException {
        Claim claim = submit(arjun, "800");
        approvalService.approve(claim, rohit, "ok");
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
        approvalService.approve(claim, rohit, "ok");
        assertThrows(UnauthorizedActionException.class, () -> approvalService.markReimbursed(claim, rohit));
    }

    @Test
    void findClaimLoadsById() throws ReimbursementException {
        Claim claim = submit(arjun, "800");
        assertEquals(claim, approvalService.findClaim(claim.getId()));
    }

    @Test
    void findClaimWithUnknownIdGivesClearError() {
        assertThrows(ReimbursementException.class, () -> approvalService.findClaim(999));
    }

    @Test
    void pendingListsFollowEscalation() throws ReimbursementException {
        Claim small = submit(arjun, "800");
        Claim big = submit(arjun, "9000");

        assertEquals(2, approvalService.findPendingFor(rohit).size());
        assertTrue(approvalService.findPendingFor(priya).isEmpty());
        assertTrue(approvalService.findPendingFor(neha).isEmpty());

        approvalService.approve(small, rohit, "ok");
        approvalService.approve(big, rohit, "ok");

        assertTrue(approvalService.findPendingFor(rohit).isEmpty());
        assertEquals(1, approvalService.findPendingFor(neha).size());
        assertEquals(big, approvalService.findPendingFor(neha).get(0));
    }

    @Test
    void resubmittedClaimNeedsManagerAgain() throws ReimbursementException {
        Claim claim = submit(arjun, "9000");
        approvalService.approve(claim, rohit, "ok");
        approvalService.reject(claim, neha, "Missing hotel bill");
        claimService.resubmit(claim, arjun);

        assertThrows(InvalidClaimStateException.class, () -> approvalService.approve(claim, neha, "ok"));
    }

    private Claim submit(Employee owner, String amount) throws ReimbursementException {
        receiptCounter++;
        Claim claim = new MedicalClaim(owner, new BigDecimal(amount), LocalDate.now(), "M" + receiptCounter, "Clinic");
        claimService.submit(claim);
        return claim;
    }
}
