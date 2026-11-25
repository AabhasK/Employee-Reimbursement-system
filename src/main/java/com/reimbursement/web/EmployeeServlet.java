package com.reimbursement.web;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.model.Claim;
import com.reimbursement.model.ClaimFactory;
import com.reimbursement.model.ClaimStatus;
import com.reimbursement.model.Employee;
import com.reimbursement.model.ProofKind;
import com.reimbursement.service.ApprovalService;
import com.reimbursement.service.ClaimService;
import com.reimbursement.service.ProofService;
import com.reimbursement.service.ProofUpload;
import com.reimbursement.service.ReportService;
import com.reimbursement.util.AppContext;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

// Tomcat stops anything above 10 MB. ProofService gives a clear message above 5 MB
@MultipartConfig(maxFileSize = 10 * 1024 * 1024, maxRequestSize = 25 * 1024 * 1024)
@WebServlet("/employee")
public class EmployeeServlet extends HttpServlet {

    private final ClaimService claimService = AppContext.getClaimService();
    private final ApprovalService approvalService = AppContext.getApprovalService();
    private final ProofService proofService = AppContext.getProofService();
    private final ReportService reportService = AppContext.getReportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Employee user = AuthFilter.userOf(request);
        ClaimForm form = ClaimForm.empty();

        String editId = request.getParameter("edit");
        if (editId != null) {
            try {
                Claim claim = claimService.findOwnClaim(ClaimForm.parseClaimId(editId), user);
                claim.checkCanMoveTo(ClaimStatus.SUBMITTED);
                form = ClaimForm.from(claim);
                request.setAttribute("editingType", claim.getType());
                request.setAttribute("rejectReason", claim.getRejectReason());
                request.setAttribute("attached", proofService.findFor(claim));
            } catch (ReimbursementException e) {
                request.setAttribute("error", Messages.describe(e));
            }
        }
        show(request, response, user, form);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Employee user = AuthFilter.userOf(request);
        ClaimForm form = ClaimForm.empty();
        try {
            form = ClaimForm.from(request);
            List<ProofUpload> uploads = readUploads(request);
            if (form.isEditing()) {
                Claim claim = claimService.findOwnClaim(form.toClaimId(), user);
                request.setAttribute("editingType", claim.getType());
                request.setAttribute("rejectReason", claim.getRejectReason());
                request.setAttribute("attached", proofService.findFor(claim));
                String receipt = claim.getReceiptNo();
                form.copyInto(claim);
                if (request.getParameter("receiptNo") == null) {
                    claim.setReceiptNo(receipt);
                }
                claimService.resubmit(claim, user, uploads);
                Views.flash(request, "Claim #" + claim.getId() + " resubmitted");
            } else {
                Claim claim = form.toNewClaim(user);
                claimService.submit(claim, uploads);
                Views.flash(request, "Claim #" + claim.getId() + " submitted");
            }
            response.sendRedirect(request.getContextPath() + "/employee");
        } catch (ReimbursementException e) {
            request.setAttribute("error", Messages.describe(e));
            show(request, response, user, form);
        } catch (IllegalStateException | ServletException e) {
            // Thrown by Tomcat when an upload is bigger than @MultipartConfig allows
            request.setAttribute("error", "Each document must be 5 MB or smaller");
            show(request, response, user, form);
        }
    }

    private List<ProofUpload> readUploads(HttpServletRequest request) throws IOException, ServletException {
        List<ProofUpload> uploads = new ArrayList<>();
        for (int i = 1; i <= ProofService.MAX_FILES; i++) {
            Part part = request.getPart("proof" + i);
            if (part == null || part.getSize() == 0) {
                continue;
            }
            byte[] bytes;
            try (InputStream in = part.getInputStream()) {
                bytes = in.readAllBytes();
            }
            ProofKind kind = ProofKind.parse(request.getParameter("kind" + i));
            uploads.add(new ProofUpload(kind, part.getSubmittedFileName(), part.getContentType(), bytes));
        }
        return uploads;
    }

    private void show(HttpServletRequest request, HttpServletResponse response, Employee user, ClaimForm form)
            throws IOException {
        List<Claim> claims = new ArrayList<>();
        try {
            claims = claimService.findClaimsOf(user);
            request.setAttribute("withFinance", approvalService.findWaitingForFinance(claims));
            request.setAttribute("proofs", proofService.findFor(claims));
        } catch (ReimbursementException e) {
            request.setAttribute("error", Messages.describe(e));
            request.setAttribute("withFinance", Set.of());
            request.setAttribute("proofs", Map.of());
        }
        request.setAttribute("claims", claims);
        request.setAttribute("counts", reportService.countByStatus(claims));
        request.setAttribute("totals", reportService.totalByStatus(claims));
        request.setAttribute("form", form);
        List<Claim> rules = claimTypes(user, ClaimFactory.typesFor(user));
        String editingType = (String) request.getAttribute("editingType");
        request.setAttribute("types", editingType == null ? rules : claimTypes(user, List.of(editingType)));
        request.setAttribute("rules", rules);
        request.setAttribute("maxProofs", ProofService.MAX_FILES);
        request.setAttribute("today", LocalDate.now().toString());
        Views.render("employee", request, response);
    }

    private List<Claim> claimTypes(Employee user, List<String> available) {
        List<Claim> types = new ArrayList<>();
        for (String type : available) {
            types.add(ClaimFactory.create(type, user, BigDecimal.ZERO, null, null, null));
        }
        return types;
    }
}
