package com.reimbursement.web;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.model.Employee;
import com.reimbursement.service.ReportService;
import com.reimbursement.service.ReportSort;
import com.reimbursement.util.AppContext;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/reports/download")
public class ReportDownloadServlet extends HttpServlet {

    private static final File REPORT_FOLDER = new File("reports");

    private final ReportService reportService = AppContext.getReportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Employee user = AuthFilter.userOf(request);
        ReportSort sort = ReportSort.parse(request.getParameter("sort"));
        File file;
        try {
            file = reportService.export(user, sort, REPORT_FOLDER);
        } catch (ReimbursementException e) {
            Views.flashError(request, Messages.describe(e));
            response.sendRedirect(request.getContextPath() + "/reports");
            return;
        } catch (IOException e) {
            Views.flashError(request, "Could not write the report file: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/reports");
            return;
        }

        response.setContentType("text/plain;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
        response.setContentLengthLong(file.length());
        Files.copy(file.toPath(), response.getOutputStream());
    }
}
