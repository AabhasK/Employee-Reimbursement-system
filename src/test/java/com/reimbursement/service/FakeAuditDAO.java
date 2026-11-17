package com.reimbursement.service;

import java.util.ArrayList;
import java.util.List;

import com.reimbursement.dao.AuditDAO;
import com.reimbursement.model.AuditEntry;

class FakeAuditDAO implements AuditDAO {

    private final List<AuditEntry> entries = new ArrayList<>();

    @Override
    public void save(AuditEntry entry) {
        entries.add(entry);
    }

    @Override
    public List<AuditEntry> findByClaim(int claimId) {
        List<AuditEntry> result = new ArrayList<>();
        for (AuditEntry entry : entries) {
            if (entry.getClaimId() == claimId) {
                result.add(entry);
            }
        }
        return result;
    }

    AuditEntry last(int claimId) {
        List<AuditEntry> history = findByClaim(claimId);
        return history.get(history.size() - 1);
    }
}
