package com.reimbursement.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.reimbursement.dao.ClaimDAO;
import com.reimbursement.exception.DataAccessException;
import com.reimbursement.model.Claim;
import com.reimbursement.model.ClaimStatus;

class FakeClaimDAO implements ClaimDAO {

    private final Map<Integer, Claim> claims = new HashMap<>();
    private int nextId = 1;

    @Override
    public void save(Claim claim) {
        claim.setId(nextId++);
        claims.put(claim.getId(), claim);
    }

    @Override
    public void update(Claim claim) throws DataAccessException {
        if (!claims.containsKey(claim.getId())) {
            throw new DataAccessException("No claim with id " + claim.getId());
        }
        claims.put(claim.getId(), claim);
    }

    @Override
    public Optional<Claim> findById(int id) {
        return Optional.ofNullable(claims.get(id));
    }

    @Override
    public List<Claim> findByEmployee(int employeeId) {
        List<Claim> result = new ArrayList<>();
        for (Claim claim : claims.values()) {
            if (claim.getEmployee().getId() == employeeId) {
                result.add(claim);
            }
        }
        return result;
    }

    @Override
    public List<Claim> findByStatus(ClaimStatus status) {
        List<Claim> result = new ArrayList<>();
        for (Claim claim : claims.values()) {
            if (claim.getStatus() == status) {
                result.add(claim);
            }
        }
        return result;
    }

    @Override
    public List<Claim> findAll() {
        return new ArrayList<>(claims.values());
    }

    @Override
    public boolean isReceiptUsed(String receiptNo, int ignoreClaimId, boolean university) {
        for (Claim claim : claims.values()) {
            if (claim.getId() != ignoreClaimId && receiptNo.equals(claim.getReceiptNo())
                    && claim.getEmployee().isUniversity() == university) {
                return true;
            }
        }
        return false;
    }

    int count() {
        return claims.size();
    }
}
