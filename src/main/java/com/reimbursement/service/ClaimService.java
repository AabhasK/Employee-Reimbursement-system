package com.reimbursement.service;

import java.util.List;

import com.reimbursement.dao.AuditDAO;
import com.reimbursement.dao.ClaimDAO;
import com.reimbursement.exception.DataAccessException;
import com.reimbursement.exception.DuplicateReceiptException;
import com.reimbursement.exception.MissingReceiptException;
import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.exception.UnauthorizedActionException;
import com.reimbursement.model.AuditEntry;
import com.reimbursement.model.Claim;
import com.reimbursement.model.ClaimFactory;
import com.reimbursement.model.ClaimStatus;
import com.reimbursement.model.Employee;
import com.reimbursement.model.ProofDocument;
import com.reimbursement.model.ProofKind;

public class ClaimService {

    private final ClaimDAO claimDAO;
    private final AuditDAO auditDAO;
    private final ProofService proofService;

    public ClaimService(ClaimDAO claimDAO, AuditDAO auditDAO, ProofService proofService) {
        this.claimDAO = claimDAO;
        this.auditDAO = auditDAO;
        this.proofService = proofService;
    }

    public void submit(Claim claim) throws ReimbursementException {
        submit(claim, List.of());
    }

    public void submit(Claim claim, List<ProofUpload> uploads) throws ReimbursementException {
        if (!ClaimFactory.allowsNewType(claim.getType(), claim.getEmployee())) {
            throw new ReimbursementException("This expense type is unavailable for your account");
        }
        cleanReceipt(claim);
        claim.validate();
        checkReceiptNotUsed(claim);
        proofService.checkUploads(uploads);
        checkReceiptFile(claim, List.of(), uploads);
        claimDAO.save(claim);
        proofService.saveUploads(claim, uploads);
        auditDAO.save(new AuditEntry(claim.getId(), claim.getEmployee().getId(),
                null, ClaimStatus.SUBMITTED, "Submitted"));
    }

    public void resubmit(Claim claim, Employee actor) throws ReimbursementException {
        resubmit(claim, actor, List.of());
    }

    public void resubmit(Claim claim, Employee actor, List<ProofUpload> uploads) throws ReimbursementException {
        if (claim.getEmployee().getId() != actor.getId()) {
            throw new UnauthorizedActionException("Only the person who made a claim can resubmit it");
        }
        claim.checkCanMoveTo(ClaimStatus.SUBMITTED);
        cleanReceipt(claim);
        claim.validate();
        checkReceiptNotUsed(claim);
        proofService.checkUploads(uploads);
        List<ProofDocument> existing = proofService.findFor(claim);
        if (existing.size() + uploads.size() > ProofService.MAX_FILES) {
            throw new ReimbursementException("A claim can have at most " + ProofService.MAX_FILES + " documents");
        }
        checkReceiptFile(claim, existing, uploads);

        ClaimStatus from = claim.getStatus();
        claim.moveTo(ClaimStatus.SUBMITTED);
        claim.setResubmitCount(claim.getResubmitCount() + 1);
        claim.setRejectReason(null);
        // The manager has to look at the documents again
        claim.setProofChecked(false);
        claimDAO.update(claim);
        proofService.saveUploads(claim, uploads);
        auditDAO.save(new AuditEntry(claim.getId(), actor.getId(), from, ClaimStatus.SUBMITTED, "Resubmitted"));
    }

    private void checkReceiptFile(Claim claim, List<ProofDocument> existing, List<ProofUpload> uploads)
            throws MissingReceiptException {
        if (!claim.needsProof()) {
            return;
        }
        for (ProofDocument doc : existing) {
            if (doc.getKind() != ProofKind.APPROVAL_LETTER) {
                return;
            }
        }
        for (ProofUpload upload : uploads) {
            if (upload.getKind() != ProofKind.APPROVAL_LETTER) {
                return;
            }
        }
        throw new MissingReceiptException("Attach the bill or ticket as a PDF or photo. "
                + claim.getTypeName() + " claims like this one need a receipt document");
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
        if (receipt != null && claimDAO.isReceiptUsed(receipt, claim.getId(), claim.getEmployee().isUniversity())) {
            throw new DuplicateReceiptException("Receipt " + receipt
                    + " is already used on another claim. Each receipt can be claimed once");
        }
    }
}
