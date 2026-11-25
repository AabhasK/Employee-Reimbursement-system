package com.reimbursement.web;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.exception.UnauthorizedActionException;
import com.reimbursement.model.Employee;
import com.reimbursement.model.ProofDocument;
import com.reimbursement.service.ProofService;
import com.reimbursement.util.AppContext;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Opens one uploaded document, after checking the viewer may see it
@WebServlet("/proof")
public class ProofServlet extends HttpServlet {

    private final ProofService proofService = AppContext.getProofService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Employee user = AuthFilter.userOf(request);
        ProofDocument proof;
        try {
            proof = proofService.findForViewer(ClaimForm.parseClaimId(request.getParameter("id")), user);
        } catch (UnauthorizedActionException e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        } catch (ReimbursementException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        File file = proofService.fileOf(proof);
        if (!file.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String safeName = proof.getOriginalName().replaceAll("[^A-Za-z0-9._ -]", "_");
        response.setContentType(proof.getContentType());
        response.setHeader("Content-Disposition", "inline; filename=\"" + safeName + "\"");
        // Stops the browser from guessing a different type than the one we checked
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setContentLengthLong(file.length());
        Files.copy(file.toPath(), response.getOutputStream());
    }
}
