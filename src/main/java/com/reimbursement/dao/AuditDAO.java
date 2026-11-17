package com.reimbursement.dao;

import java.util.List;

import com.reimbursement.exception.DataAccessException;
import com.reimbursement.model.AuditEntry;

public interface AuditDAO {

    void save(AuditEntry entry) throws DataAccessException;

    List<AuditEntry> findByClaim(int claimId) throws DataAccessException;
}
