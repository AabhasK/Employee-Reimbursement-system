package com.reimbursement.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.reimbursement.dao.AuditDAO;
import com.reimbursement.dao.ClaimDAO;
import com.reimbursement.exception.DataAccessException;
import com.reimbursement.exception.InvalidClaimStateException;
import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.exception.SelfApprovalException;
import com.reimbursement.exception.UnauthorizedActionException;
import com.reimbursement.model.AuditEntry;
import com.reimbursement.model.Claim;
import com.reimbursement.model.ClaimStatus;
import com.reimbursement.model.Employee;
import com.reimbursement.model.Role;

public class ApprovalService implements Approvable {

    public static final BigDecimal FINANCE_THRESHOLD = new BigDecimal("5000");

    private final ClaimDAO claimDAO;
    private final AuditDAO auditDAO;

    public ApprovalService(ClaimDAO claimDAO, AuditDAO auditDAO) {
        this.claimDAO = claimDAO;
        this.auditDAO = auditDAO;
    }

    @Override
    public void approve(Claim claim, Employee approver, String note) throws ReimbursementException {
        checkCanAct(claim, approver);
        claim.checkCanMoveTo(ClaimStatus.APPROVED);
        checkLength(note, "Note");
        boolean managerApproved = isManagerApproved(claim);

        if (approver.getRole() == Role.MANAGER) {
            if (managerApproved) {
                throw new InvalidClaimStateException("Claim #" + claim.getId()
                        + " is already approved by you and is waiting for finance");
            }
            if (needsFinance(claim)) {
                auditDAO.save(new AuditEntry(claim.getId(), approver.getId(),
                        ClaimStatus.SUBMITTED, ClaimStatus.SUBMITTED, note));
                return;
            }
        } else {
            if (!needsFinance(claim)) {
                throw new UnauthorizedActionException("Claims up to Rs " + FINANCE_THRESHOLD
                        + " are approved by the employee's manager, not finance");
            }
            if (!managerApproved) {
                throw new InvalidClaimStateException("Claim #" + claim.getId()
                        + " needs the manager's approval before finance can approve it");
            }
        }
        moveTo(claim, approver, ClaimStatus.APPROVED, note);
    }

    @Override
    public void reject(Claim claim, Employee approver, String reason) throws ReimbursementException {
        checkCanAct(claim, approver);
        if (reason == null || reason.isBlank()) {
            throw new ReimbursementException("Write a reason before rejecting a claim");
        }
        checkLength(reason, "Reason");
        claim.checkCanMoveTo(ClaimStatus.REJECTED);
        claim.setRejectReason(reason);
        moveTo(claim, approver, ClaimStatus.REJECTED, reason);
    }

    public void markReimbursed(Claim claim, Employee actor) throws ReimbursementException {
        if (actor.getRole() != Role.FINANCE) {
            throw new UnauthorizedActionException("Only finance can mark a claim as reimbursed");
        }
        if (claim.getEmployee().getId() == actor.getId()) {
            throw new SelfApprovalException("You cannot reimburse your own claim");
        }
        moveTo(claim, actor, ClaimStatus.REIMBURSED, "Reimbursed");
    }

    public Claim findClaim(int claimId) throws ReimbursementException {
        return claimDAO.findById(claimId)
                .orElseThrow(() -> new ReimbursementException("Claim #" + claimId + " was not found"));
    }

    public List<Claim> findPendingFor(Employee approver) throws DataAccessException {
        List<Claim> pending = new ArrayList<>();
        for (Claim claim : claimDAO.findByStatus(ClaimStatus.SUBMITTED)) {
            if (isWaitingFor(claim, approver)) {
                pending.add(claim);
            }
        }
        return pending;
    }

    public List<Claim> findReadyToReimburse() throws DataAccessException {
        return claimDAO.findByStatus(ClaimStatus.APPROVED);
    }

    public boolean needsFinance(Claim claim) {
        return claim.getAmount().compareTo(FINANCE_THRESHOLD) > 0;
    }

    private boolean isWaitingFor(Claim claim, Employee approver) throws DataAccessException {
        Employee owner = claim.getEmployee();
        if (owner.getId() == approver.getId()) {
            return false;
        }
        if (approver.getRole() == Role.MANAGER) {
            return isManagerOf(approver, owner) && !isManagerApproved(claim);
        }
        if (approver.getRole() == Role.FINANCE) {
            return needsFinance(claim) && isManagerApproved(claim);
        }
        return false;
    }

    private void checkCanAct(Claim claim, Employee approver) throws ReimbursementException {
        Employee owner = claim.getEmployee();
        if (owner.getId() == approver.getId()) {
            throw new SelfApprovalException("You cannot approve or reject your own claim");
        }
        if (!isManagerOf(approver, owner) && approver.getRole() != Role.FINANCE) {
            throw new UnauthorizedActionException("Only " + owner.getName()
                    + "'s manager or finance can act on this claim");
        }
    }

    private void checkLength(String text, String field) throws ReimbursementException {
        if (text != null && text.length() > Claim.MAX_TEXT_LENGTH) {
            throw new ReimbursementException(field + " can be at most " + Claim.MAX_TEXT_LENGTH + " characters");
        }
    }

    private boolean isManagerOf(Employee approver, Employee owner) {
        return approver.getRole() == Role.MANAGER
                && owner.getManagerId() != null
                && owner.getManagerId() == approver.getId();
    }

    private boolean isManagerApproved(Claim claim) throws DataAccessException {
        List<AuditEntry> history = auditDAO.findByClaim(claim.getId());
        if (history.isEmpty()) {
            return false;
        }
        AuditEntry last = history.get(history.size() - 1);
        // A manager approval that still needs finance is logged as SUBMITTED to SUBMITTED
        return last.getFromStatus() == ClaimStatus.SUBMITTED && last.getToStatus() == ClaimStatus.SUBMITTED;
    }

    private void moveTo(Claim claim, Employee actor, ClaimStatus next, String note) throws ReimbursementException {
        ClaimStatus from = claim.getStatus();
        claim.moveTo(next);
        claimDAO.update(claim);
        auditDAO.save(new AuditEntry(claim.getId(), actor.getId(), from, next, note));
    }
}
