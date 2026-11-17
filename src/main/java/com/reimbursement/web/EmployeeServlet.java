package com.reimbursement.web;

import java.io.IOException;
import java.time.LocalDate;

import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.model.Claim;
import com.reimbursement.model.ClaimFactory;
import com.reimbursement.model.ClaimStatus;
import com.reimbursement.model.Employee;
import com.reimbursement.service.ClaimService;
import com.reimbursement.util.AppContext;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/employee")
public class EmployeeServlet extends HttpServlet {

    private final ClaimService claimService = AppContext.getClaimService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Employee user = AuthFilter.userOf(request);
        ClaimForm form = ClaimForm.empty();

        String editId = request.getParameter("edit");
        if (editId != null) {
            try {
                ClaimForm idOnly = new ClaimForm(editId, "", "", "", "", "");
                Claim claim = claimService.findOwnClaim(idOnly.toClaimId(), user);
                claim.checkCanMoveTo(ClaimStatus.SUBMITTED);
                form = ClaimForm.from(claim);
                request.setAttribute("rejectReason", claim.getRejectReason());
            } catch (ReimbursementException e) {
                request.setAttribute("error", Messages.describe(e));
            }
        }
        show(request, response, user, form);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Employee user = AuthFilter.userOf(request);
        ClaimForm form = ClaimForm.from(request);
        try {
            if (form.isEditing()) {
                Claim claim = claimService.findOwnClaim(form.toClaimId(), user);
                form.copyInto(claim);
                claimService.resubmit(claim, user);
                Views.flash(request, "Claim #" + claim.getId() + " resubmitted");
            } else {
                Claim claim = form.toNewClaim(user);
                claimService.submit(claim);
                Views.flash(request, "Claim #" + claim.getId() + " submitted");
            }
            response.sendRedirect(request.getContextPath() + "/employee");
        } catch (ReimbursementException e) {
            request.setAttribute("error", Messages.describe(e));
            show(request, response, user, form);
        }
    }

    private void show(HttpServletRequest request, HttpServletResponse response, Employee user, ClaimForm form)
            throws IOException {
        try {
            request.setAttribute("claims", claimService.findClaimsOf(user));
        } catch (ReimbursementException e) {
            request.setAttribute("error", Messages.describe(e));
        }
        request.setAttribute("form", form);
        request.setAttribute("types", ClaimFactory.TYPES);
        request.setAttribute("today", LocalDate.now().toString());
        Views.render("employee", request, response);
    }
}
