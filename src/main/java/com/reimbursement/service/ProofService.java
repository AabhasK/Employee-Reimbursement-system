package com.reimbursement.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.reimbursement.dao.ClaimDAO;
import com.reimbursement.dao.ProofDAO;
import com.reimbursement.exception.DataAccessException;
import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.exception.UnauthorizedActionException;
import com.reimbursement.model.Claim;
import com.reimbursement.model.Employee;
import com.reimbursement.model.ProofDocument;
import com.reimbursement.model.Role;
import com.reimbursement.util.ProofStore;

public class ProofService {

    public static final int MAX_FILES = 2;

    private final ProofDAO proofDAO;
    private final ProofStore proofStore;
    private final ClaimDAO claimDAO;

    public ProofService(ProofDAO proofDAO, ProofStore proofStore, ClaimDAO claimDAO) {
        this.proofDAO = proofDAO;
        this.proofStore = proofStore;
        this.claimDAO = claimDAO;
    }

    public void checkUploads(List<ProofUpload> uploads) throws ReimbursementException {
        if (uploads.size() > MAX_FILES) {
            throw new ReimbursementException("Attach at most " + MAX_FILES + " documents to one claim");
        }
        for (ProofUpload upload : uploads) {
            if (!ProofStore.isAllowedType(upload.getContentType())) {
                throw new ReimbursementException(upload.getFileName() + " is not a PDF, JPG, PNG or WEBP file");
            }
            if (upload.getBytes().length > ProofStore.MAX_BYTES) {
                throw new ReimbursementException(upload.getFileName() + " is larger than 5 MB");
            }
            if (!ProofStore.contentMatches(upload.getContentType(), upload.getBytes())) {
                throw new ReimbursementException(upload.getFileName() + " does not look like a real "
                        + upload.getContentType() + " file");
            }
        }
    }

    public void saveUploads(Claim claim, List<ProofUpload> uploads) throws ReimbursementException {
        for (ProofUpload upload : uploads) {
            String storedName;
            try {
                storedName = proofStore.save(upload.getBytes(), upload.getContentType());
            } catch (IOException e) {
                throw new ReimbursementException("Could not save " + upload.getFileName(), e);
            }
            String name = upload.getFileName().length() > 255 ? upload.getFileName().substring(0, 255)
                    : upload.getFileName();
            proofDAO.save(new ProofDocument(claim.getId(), upload.getKind(), name, storedName,
                    upload.getContentType(), upload.getBytes().length));
        }
    }

    public List<ProofDocument> findFor(Claim claim) throws DataAccessException {
        return proofDAO.findByClaim(claim.getId());
    }

    public Map<Integer, List<ProofDocument>> findFor(List<Claim> claims) throws DataAccessException {
        Map<Integer, List<ProofDocument>> proofs = new HashMap<>();
        for (Claim claim : claims) {
            proofs.put(claim.getId(), new ArrayList<>(proofDAO.findByClaim(claim.getId())));
        }
        return proofs;
    }

    // Only the owner, the owner's manager and finance may open a document
    public ProofDocument findForViewer(int proofId, Employee viewer) throws ReimbursementException {
        ProofDocument proof = proofDAO.findById(proofId)
                .orElseThrow(() -> new ReimbursementException("Document #" + proofId + " was not found"));
        Claim claim = claimDAO.findById(proof.getClaimId())
                .orElseThrow(() -> new ReimbursementException("Claim #" + proof.getClaimId() + " was not found"));
        Employee owner = claim.getEmployee();
        if (owner.isUniversity() != viewer.isUniversity()) {
            throw new UnauthorizedActionException("This document belongs to another organization");
        }
        boolean isOwner = owner.getId() == viewer.getId();
        boolean isManager = viewer.getRole() == Role.MANAGER && owner.getManagerId() != null
                && owner.getManagerId() == viewer.getId();
        if (!isOwner && !isManager && viewer.getRole() != Role.FINANCE) {
            throw new UnauthorizedActionException("You cannot open documents of " + owner.getName() + "'s claims");
        }
        return proof;
    }

    public File fileOf(ProofDocument proof) {
        return proofStore.fileFor(proof.getStoredName());
    }
}
