package com.reimbursement.service;

import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.model.Claim;
import com.reimbursement.model.Employee;

public interface Approvable {

    void approve(Claim claim, Employee approver, String note) throws ReimbursementException;

    void reject(Claim claim, Employee approver, String reason) throws ReimbursementException;
}
