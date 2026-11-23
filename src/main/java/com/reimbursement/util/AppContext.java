package com.reimbursement.util;

import com.reimbursement.dao.AuditDAO;
import com.reimbursement.dao.ClaimDAO;
import com.reimbursement.dao.EmployeeDAO;
import com.reimbursement.dao.jdbc.JdbcAuditDAO;
import com.reimbursement.dao.jdbc.JdbcClaimDAO;
import com.reimbursement.dao.jdbc.JdbcEmployeeDAO;
import com.reimbursement.service.ApprovalService;
import com.reimbursement.service.ClaimService;
import com.reimbursement.service.LoginService;
import com.reimbursement.service.ReportService;

public class AppContext {

    private static final EmployeeDAO employeeDAO = new JdbcEmployeeDAO();
    private static final ClaimDAO claimDAO = new JdbcClaimDAO(employeeDAO);
    private static final AuditDAO auditDAO = new JdbcAuditDAO();

    private static final LoginService loginService = new LoginService(employeeDAO);
    private static final ClaimService claimService = new ClaimService(claimDAO, auditDAO);
    private static final ApprovalService approvalService = new ApprovalService(claimDAO, auditDAO);
    private static final ReportService reportService = new ReportService(claimDAO, new ReportWriter());

    private AppContext() {
    }

    public static LoginService getLoginService() {
        return loginService;
    }

    public static ClaimService getClaimService() {
        return claimService;
    }

    public static ApprovalService getApprovalService() {
        return approvalService;
    }

    public static ReportService getReportService() {
        return reportService;
    }
}
