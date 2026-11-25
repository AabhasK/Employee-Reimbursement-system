package com.reimbursement.dao;

import java.util.List;
import java.util.Optional;

import com.reimbursement.exception.DataAccessException;
import com.reimbursement.model.Claim;
import com.reimbursement.model.ClaimStatus;

public interface ClaimDAO {

    void save(Claim claim) throws DataAccessException;

    void update(Claim claim) throws DataAccessException;

    Optional<Claim> findById(int id) throws DataAccessException;

    List<Claim> findByEmployee(int employeeId) throws DataAccessException;

    List<Claim> findByStatus(ClaimStatus status) throws DataAccessException;

    List<Claim> findAll() throws DataAccessException;

    boolean isReceiptUsed(String receiptNo, int ignoreClaimId, boolean university) throws DataAccessException;
}
