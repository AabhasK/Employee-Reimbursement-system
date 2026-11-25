package com.reimbursement.dao;

import java.util.List;
import java.util.Optional;

import com.reimbursement.exception.DataAccessException;
import com.reimbursement.model.ProofDocument;

public interface ProofDAO {

    void save(ProofDocument proof) throws DataAccessException;

    Optional<ProofDocument> findById(int id) throws DataAccessException;

    List<ProofDocument> findByClaim(int claimId) throws DataAccessException;
}
