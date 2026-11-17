package com.reimbursement.web;

import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.model.Claim;
import com.reimbursement.model.Employee;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

@WebServlet("/finance")
public class FinanceServlet extends ApprovalServlet {

    @Override
    protected String page() {
        return "finance";
    }

    @Override
    protected String handle(String action, Claim claim, Employee user, String note)
            throws ReimbursementException {
        if ("reimburse".equals(action)) {
            approvalService.markReimbursed(claim, user);
            return "Claim #" + claim.getId() + " marked as reimbursed";
        }
        return super.handle(action, claim, user, note);
    }

    @Override
    protected void addPageData(HttpServletRequest request, Employee user) throws ReimbursementException {
        request.setAttribute("ready", approvalService.findReadyToReimburse());
    }
}
