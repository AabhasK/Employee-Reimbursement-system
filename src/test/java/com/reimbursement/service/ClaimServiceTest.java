package com.reimbursement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.reimbursement.exception.ClaimLimitExceededException;
import com.reimbursement.exception.DuplicateReceiptException;
import com.reimbursement.exception.InvalidClaimStateException;
import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.exception.UnauthorizedActionException;
import com.reimbursement.model.AuditEntry;
import com.reimbursement.model.Claim;
import com.reimbursement.model.ClaimStatus;
import com.reimbursement.model.Employee;
import com.reimbursement.model.FoodClaim;
import com.reimbursement.model.Role;
import com.reimbursement.model.TravelClaim;

class ClaimServiceTest {

    private final Employee rohit = new Employee(2, "Rohit Sharma", "rohit@company.com", "h", Role.MANAGER, null);
    private final Employee arjun = new Employee(4, "Arjun Singh", "arjun@company.com", "h", Role.EMPLOYEE, 2);
    private final Employee simran = new Employee(5, "Simran Kaur", "simran@company.com", "h", Role.EMPLOYEE, 2);

    private FakeClaimDAO claimDAO;
    private FakeAuditDAO auditDAO;
    private ClaimService claimService;
    private ApprovalService approvalService;

    @BeforeEach
    void setUp() {
        claimDAO = new FakeClaimDAO();
        auditDAO = new FakeAuditDAO();
        claimService = new ClaimService(claimDAO, auditDAO);
        approvalService = new ApprovalService(claimDAO, auditDAO);
    }

    @Test
    void submitSavesClaimAndWritesAudit() throws ReimbursementException {
        Claim claim = food(arjun, "300", "R1");
        claimService.submit(claim);

        assertEquals(1, claimDAO.count());
        AuditEntry entry = auditDAO.last(claim.getId());
        assertNull(entry.getFromStatus());
        assertEquals(ClaimStatus.SUBMITTED, entry.getToStatus());
    }

    @Test
    void invalidClaimIsNotSaved() {
        Claim claim = food(arjun, "3000", "R1");
        assertThrows(ClaimLimitExceededException.class, () -> claimService.submit(claim));
        assertEquals(0, claimDAO.count());
    }

    @Test
    void duplicateReceiptIsBlocked() throws ReimbursementException {
        claimService.submit(food(arjun, "300", "R1"));
        Claim copy = food(simran, "300", " R1 ");
        assertThrows(DuplicateReceiptException.class, () -> claimService.submit(copy));
    }

    @Test
    void blankReceiptIsStoredAsNull() throws ReimbursementException {
        Claim first = food(arjun, "100", "  ");
        Claim second = food(arjun, "100", "");
        claimService.submit(first);
        claimService.submit(second);

        assertNull(first.getReceiptNo());
        assertEquals(2, claimDAO.count());
    }

    @Test
    void rejectedClaimCanBeResubmitted() throws ReimbursementException {
        Claim claim = travel(arjun, "2000", "T1");
        claimService.submit(claim);
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
        claimService.submit(claim);
        approvalService.reject(claim, rohit, "Amount looks high");

        claim.setAmount(new BigDecimal("1800"));
        claimService.resubmit(claim, arjun);

        assertEquals(ClaimStatus.SUBMITTED, claim.getStatus());
    }

    @Test
    void onlyRejectedClaimCanBeResubmitted() throws ReimbursementException {
        Claim claim = food(arjun, "300", "R1");
        claimService.submit(claim);
        assertThrows(InvalidClaimStateException.class, () -> claimService.resubmit(claim, arjun));
    }

    @Test
    void onlyOwnerCanResubmit() throws ReimbursementException {
        Claim claim = food(arjun, "300", "R1");
        claimService.submit(claim);
        approvalService.reject(claim, rohit, "No bill");
        assertThrows(UnauthorizedActionException.class, () -> claimService.resubmit(claim, simran));
    }

    @Test
    void ownerCanLoadOwnClaim() throws ReimbursementException {
        Claim claim = food(arjun, "300", "R1");
        claimService.submit(claim);
        assertEquals(claim, claimService.findOwnClaim(claim.getId(), arjun));
    }

    @Test
    void otherEmployeeCannotLoadTheClaim() throws ReimbursementException {
        Claim claim = food(arjun, "300", "R1");
        claimService.submit(claim);
        assertThrows(UnauthorizedActionException.class, () -> claimService.findOwnClaim(claim.getId(), simran));
    }

    @Test
    void missingClaimGivesClearError() {
        assertThrows(ReimbursementException.class, () -> claimService.findOwnClaim(99, arjun));
    }

    @Test
    void findClaimsOfReturnsOnlyThatEmployee() throws ReimbursementException {
        claimService.submit(food(arjun, "100", "A1"));
        claimService.submit(food(arjun, "200", "A2"));
        claimService.submit(food(simran, "300", "S1"));

        assertEquals(2, claimService.findClaimsOf(arjun).size());
        assertTrue(claimService.findClaimsOf(rohit).isEmpty());
    }

    private Claim food(Employee owner, String amount, String receipt) {
        return new FoodClaim(owner, new BigDecimal(amount), LocalDate.now(), receipt, "Meal");
    }

    private Claim travel(Employee owner, String amount, String receipt) {
        return new TravelClaim(owner, new BigDecimal(amount), LocalDate.now(), receipt, "Cab");
    }
}
