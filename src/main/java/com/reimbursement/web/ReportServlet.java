package com.reimbursement.web;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.model.Claim;
import com.reimbursement.model.ClaimStatus;
import com.reimbursement.model.Employee;
import com.reimbursement.service.ReportService;
import com.reimbursement.service.ReportSort;
import com.reimbursement.util.AppContext;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/reports")
public class ReportServlet extends HttpServlet {

    private final ReportService reportService = AppContext.getReportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Employee user = AuthFilter.userOf(request);
        ReportSort sort = ReportSort.parse(request.getParameter("sort"));
        try {
            List<Claim> claims = reportService.sort(reportService.findClaimsFor(user), sort);
            request.setAttribute("claims", claims);
            request.setAttribute("counts", reportService.countByStatus(claims));
            Map<ClaimStatus, BigDecimal> totals = reportService.totalByStatus(claims);
            request.setAttribute("totals", totals);
            request.setAttribute("percents", reportService.percentByStatus(totals));
            request.setAttribute("withFinance", AppContext.getApprovalService().findWaitingForFinance(claims));
            request.setAttribute("proofs", AppContext.getProofService().findFor(claims));
        } catch (ReimbursementException e) {
            request.setAttribute("error", Messages.describe(e));
            request.setAttribute("claims", List.of());
            request.setAttribute("withFinance", Set.of());
        }
        request.setAttribute("sort", sort);
        request.setAttribute("sorts", ReportSort.values());
        Views.render("reports", request, response);
    }
}
