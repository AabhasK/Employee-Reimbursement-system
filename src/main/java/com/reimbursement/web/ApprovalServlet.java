package com.reimbursement.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.exception.UnauthorizedActionException;
import com.reimbursement.model.Claim;
import com.reimbursement.model.ClaimStatus;
import com.reimbursement.model.Employee;
import com.reimbursement.service.ApprovalService;
import com.reimbursement.service.ProofService;
import com.reimbursement.service.ReportService;
import com.reimbursement.util.AppContext;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class ApprovalServlet extends HttpServlet {

    protected final ApprovalService approvalService = AppContext.getApprovalService();
    protected final ReportService reportService = AppContext.getReportService();
    protected final ProofService proofService = AppContext.getProofService();

    // Name of the template and of the URL, like "manager"
    protected abstract String page();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        show(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Employee user = AuthFilter.userOf(request);
        try {
            Claim claim = approvalService.findClaim(ClaimForm.parseClaimId(request.getParameter("claimId")), user);
            boolean proofChecked = "on".equals(request.getParameter("proofChecked"));
            String message = handle(request.getParameter("action"), claim, user, request.getParameter("note"),
                    proofChecked);
            Views.flash(request, message);
            response.sendRedirect(request.getContextPath() + "/" + page());
        } catch (UnauthorizedActionException e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (ReimbursementException e) {
            request.setAttribute("error", Messages.describe(e));
            show(request, response);
        }
    }

    protected String handle(String action, Claim claim, Employee user, String note, boolean proofChecked)
            throws ReimbursementException {
        if ("approve".equals(action)) {
            approvalService.approve(claim, user, note, proofChecked);
            if (claim.getStatus() == ClaimStatus.APPROVED) {
                return "Claim #" + claim.getId() + " approved";
            }
            return "Claim #" + claim.getId() + " approved and sent to finance";
        }
        if ("reject".equals(action)) {
            approvalService.reject(claim, user, note);
            return "Claim #" + claim.getId() + " rejected";
        }
        throw new ReimbursementException("Unknown action");
    }

    protected void addPageData(HttpServletRequest request, Employee user) throws ReimbursementException {
    }

    private void show(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Employee user = AuthFilter.userOf(request);
        List<Claim> pending = new ArrayList<>();
        try {
            pending = approvalService.findPendingFor(user);
            addPageData(request, user);
        } catch (ReimbursementException e) {
            request.setAttribute("error", Messages.describe(e));
        }
        request.setAttribute("pending", pending);
        try {
            request.setAttribute("proofs", proofService.findFor(pending));
        } catch (ReimbursementException e) {
            request.setAttribute("error", Messages.describe(e));
        }
        request.setAttribute("pendingTotal", reportService.totalOf(pending));
        request.setAttribute("financeThreshold", ApprovalService.FINANCE_THRESHOLD);
        Views.render(page(), request, response);
    }
}
