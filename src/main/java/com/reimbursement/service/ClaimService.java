package com.reimbursement.service;

import java.util.List;

import com.reimbursement.dao.AuditDAO;
import com.reimbursement.dao.ClaimDAO;
import com.reimbursement.exception.DataAccessException;
import com.reimbursement.exception.DuplicateReceiptException;
import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.exception.UnauthorizedActionException;
import com.reimbursement.model.AuditEntry;
import com.reimbursement.model.Claim;
import com.reimbursement.model.ClaimStatus;
import com.reimbursement.model.Employee;

public class ClaimService {

    private final ClaimDAO claimDAO;
    private final AuditDAO auditDAO;

    public ClaimService(ClaimDAO claimDAO, AuditDAO auditDAO) {
        this.claimDAO = claimDAO;
        this.auditDAO = auditDAO;
    }

    public void submit(Claim claim) throws ReimbursementException {
        cleanReceipt(claim);
        claim.validate();
        checkReceiptNotUsed(claim);
        claimDAO.save(claim);
        auditDAO.save(new AuditEntry(claim.getId(), claim.getEmployee().getId(),
                null, ClaimStatus.SUBMITTED, "Submitted"));
    }

    public void resubmit(Claim claim, Employee actor) throws ReimbursementException {
        if (claim.getEmployee().getId() != actor.getId()) {
            throw new UnauthorizedActionException("Only the person who made a claim can resubmit it");
        }
        claim.checkCanMoveTo(ClaimStatus.SUBMITTED);
        cleanReceipt(claim);
        claim.validate();
        checkReceiptNotUsed(claim);

        ClaimStatus from = claim.getStatus();
        claim.moveTo(ClaimStatus.SUBMITTED);
        claim.setResubmitCount(claim.getResubmitCount() + 1);
        claim.setRejectReason(null);
        claimDAO.update(claim);
        auditDAO.save(new AuditEntry(claim.getId(), actor.getId(), from, ClaimStatus.SUBMITTED, "Resubmitted"));
    }

    public Claim findOwnClaim(int claimId, Employee owner) throws ReimbursementException {
        Claim claim = claimDAO.findById(claimId)
                .orElseThrow(() -> new ReimbursementException("Claim #" + claimId + " was not found"));
        if (claim.getEmployee().getId() != owner.getId()) {
            throw new UnauthorizedActionException("Claim #" + claimId + " belongs to someone else");
        }
        return claim;
    }

    public List<Claim> findClaimsOf(Employee employee) throws DataAccessException {
        return claimDAO.findByEmployee(employee.getId());
    }

    public List<AuditEntry> findHistory(Claim claim) throws DataAccessException {
        return auditDAO.findByClaim(claim.getId());
    }

    private void cleanReceipt(Claim claim) {
        String receipt = claim.getReceiptNo();
        if (receipt == null || receipt.isBlank()) {
            claim.setReceiptNo(null);
        } else {
            claim.setReceiptNo(receipt.trim());
        }
    }

    private void checkReceiptNotUsed(Claim claim) throws ReimbursementException {
        String receipt = claim.getReceiptNo();
        if (receipt != null && claimDAO.isReceiptUsed(receipt, claim.getId())) {
            throw new DuplicateReceiptException("Receipt " + receipt
                    + " is already used on another claim. Each receipt can be claimed once");
        }
    }
}
