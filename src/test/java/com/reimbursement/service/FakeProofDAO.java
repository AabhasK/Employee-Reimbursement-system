package com.reimbursement.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.reimbursement.dao.ProofDAO;
import com.reimbursement.model.ProofDocument;

class FakeProofDAO implements ProofDAO {

    private final List<ProofDocument> proofs = new ArrayList<>();
    private int nextId = 1;

    @Override
    public void save(ProofDocument proof) {
        proof.setId(nextId++);
        proofs.add(proof);
    }

    @Override
    public Optional<ProofDocument> findById(int id) {
        for (ProofDocument proof : proofs) {
            if (proof.getId() == id) {
                return Optional.of(proof);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<ProofDocument> findByClaim(int claimId) {
        List<ProofDocument> result = new ArrayList<>();
        for (ProofDocument proof : proofs) {
            if (proof.getClaimId() == claimId) {
                result.add(proof);
            }
        }
        return result;
    }
}
