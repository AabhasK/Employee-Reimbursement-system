package com.reimbursement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.reimbursement.dao.DBConnection;
import com.reimbursement.dao.jdbc.JdbcEmployeeDAO;
import com.reimbursement.dao.jdbc.JdbcSignupDAO;
import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.model.Employee;
import com.reimbursement.model.Grade;
import com.reimbursement.model.Role;
import com.reimbursement.util.SignupMailer;

class SignupServiceTest {

    @Test
    void thaparDomainMustMatchExactly() {
        assertTrue(SignupService.isThaparEmail("student.1@thapar.edu"));
        assertFalse(SignupService.isThaparEmail("student@company.com"));
        assertFalse(SignupService.isThaparEmail("student@thapar.edu.example.com"));
    }

    @Test
    void emailedCodeCreatesOnlyOneStudentAccount() throws Exception {
        assumeTrue(Files.isRegularFile(Path.of("db.properties")));
        String email = "test-" + UUID.randomUUID() + "@thapar.edu";
        String password = "long local test password";
        CapturingMailer mailer = new CapturingMailer();
        JdbcEmployeeDAO employees = new JdbcEmployeeDAO();
        SignupService signup = new SignupService(new JdbcSignupDAO(), employees, mailer,
                "rohit@thapar.edu", "a local test secret with over 32 characters", true);

        try {
            assertThrows(ReimbursementException.class, () -> signup.requestCode("employee@company.com"));
            assertEquals(email, signup.requestCode(email));
            assertEquals(email, mailer.email);
            assertEquals(email, signup.requestCode(email));
            assertEquals(1, mailer.sends);
            assertTrue(employees.findByEmail(email).isEmpty());

            String wrong = "000000".equals(mailer.code) ? "999999" : "000000";
            assertThrows(ReimbursementException.class, () -> signup.register(email, wrong, "Test Student", password));
            signup.register(email, mailer.code, "Test Student", password);
            assertThrows(ReimbursementException.class,
                    () -> signup.register(email, mailer.code, "Test Student", password));

            Employee student = new LoginService(employees).login(email, password);
            assertEquals(Grade.STUDENT, student.getGrade());
            assertEquals(Role.EMPLOYEE, student.getRole());
            assertEquals(employees.findByEmail("rohit@thapar.edu").orElseThrow().getId(), student.getManagerId());
            assertNotEquals(LoginService.hash(password), student.getPasswordHash());
        } finally {
            try (Connection con = DBConnection.getConnection();
                    PreparedStatement codes = con.prepareStatement("DELETE FROM signup_codes WHERE email = ?");
                    PreparedStatement user = con.prepareStatement("DELETE FROM employees WHERE email = ?")) {
                codes.setString(1, email);
                codes.executeUpdate();
                user.setString(1, email);
                user.executeUpdate();
            }
        }
    }

    private static class CapturingMailer extends SignupMailer {

        private String email;
        private String code;
        private int sends;

        CapturingMailer() {
            super("test-key", "signup@example.test");
        }

        @Override
        public void send(String address, String verificationCode) {
            email = address;
            code = verificationCode;
            sends++;
        }
    }
}
