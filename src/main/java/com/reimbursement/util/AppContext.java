package com.reimbursement.util;

import java.io.File;

import com.reimbursement.dao.AuditDAO;
import com.reimbursement.dao.ClaimDAO;
import com.reimbursement.dao.EmployeeDAO;
import com.reimbursement.dao.ProofDAO;
import com.reimbursement.dao.jdbc.JdbcAuditDAO;
import com.reimbursement.dao.jdbc.JdbcClaimDAO;
import com.reimbursement.dao.jdbc.JdbcEmployeeDAO;
import com.reimbursement.dao.jdbc.JdbcProofDAO;
import com.reimbursement.dao.jdbc.JdbcSignupDAO;
import com.reimbursement.service.ApprovalService;
import com.reimbursement.service.ClaimService;
import com.reimbursement.service.LoginService;
import com.reimbursement.service.ProofService;
import com.reimbursement.service.ReportService;
import com.reimbursement.service.SignupService;

public class AppContext {

    private static final EmployeeDAO employeeDAO = new JdbcEmployeeDAO();
    private static final ClaimDAO claimDAO = new JdbcClaimDAO(employeeDAO);
    private static final AuditDAO auditDAO = new JdbcAuditDAO();
    private static final ProofDAO proofDAO = new JdbcProofDAO();

    private static final ProofService proofService =
            new ProofService(proofDAO, new ProofStore(new File("uploads")), claimDAO);
    private static final LoginService loginService = new LoginService(employeeDAO);
    private static final SignupService signupService = new SignupService(new JdbcSignupDAO(), employeeDAO,
            new SignupMailer(System.getenv("RESEND_API_KEY"), System.getenv("APP_SIGNUP_FROM")),
            System.getenv("APP_THAPAR_MANAGER"), System.getenv("APP_SIGNUP_SECRET"),
            Boolean.parseBoolean(System.getenv("APP_SIGNUP_ENABLED")));
    private static final ClaimService claimService = new ClaimService(claimDAO, auditDAO, proofService);
    private static final ApprovalService approvalService = new ApprovalService(claimDAO, auditDAO, proofDAO);
    private static final ReportService reportService = new ReportService(claimDAO, new ReportWriter());

    private AppContext() {
    }

    public static LoginService getLoginService() {
        return loginService;
    }

    public static SignupService getSignupService() {
        return signupService;
    }

    public static ClaimService getClaimService() {
        return claimService;
    }

    public static ApprovalService getApprovalService() {
        return approvalService;
    }

    public static ProofService getProofService() {
        return proofService;
    }

    public static ReportService getReportService() {
        return reportService;
    }
}
