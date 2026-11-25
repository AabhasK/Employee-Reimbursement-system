package com.reimbursement.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.reimbursement.dao.EmployeeDAO;
import com.reimbursement.dao.jdbc.JdbcSignupDAO;
import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.model.Employee;
import com.reimbursement.model.Role;
import com.reimbursement.util.Passwords;
import com.reimbursement.util.SignupMailer;

public class SignupService {

    private final SecureRandom random = new SecureRandom();
    private final JdbcSignupDAO signupDAO;
    private final EmployeeDAO employees;
    private final SignupMailer mailer;
    private final String managerEmail;
    private final String secret;
    private final boolean enabled;

    public SignupService(JdbcSignupDAO signupDAO, EmployeeDAO employees, SignupMailer mailer,
            String managerEmail, String secret, boolean enabled) {
        this.signupDAO = signupDAO;
        this.employees = employees;
        this.mailer = mailer;
        this.managerEmail = managerEmail;
        this.secret = secret;
        this.enabled = enabled;
    }

    public boolean available() {
        return enabled && mailer.configured() && isThaparEmail(managerEmail) && secret != null && secret.length() >= 32;
    }

    public String requestCode(String address) throws ReimbursementException {
        checkAvailable();
        String email = address == null ? "" : address.trim().toLowerCase(Locale.ROOT);
        if (!isThaparEmail(email)) {
            throw new ReimbursementException("Use your @thapar.edu email address");
        }
        if (employees.findByEmail(email).isPresent()) {
            return email;
        }
        managerId();
        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        if (signupDAO.issue(email, digest(email, code), Instant.now().getEpochSecond())) {
            send(email, code);
        }
        return email;
    }

    public void register(String email, String code, String name, String password) throws ReimbursementException {
        checkAvailable();
        if (!isThaparEmail(email) || code == null || !code.matches("[0-9]{6}")) {
            throw new ReimbursementException("Enter the six-digit code from your email");
        }
        if (name == null || name.isBlank() || name.trim().length() > 100) {
            throw new ReimbursementException("Enter your name, up to 100 characters");
        }
        if (!Passwords.validLength(password)) {
            throw new ReimbursementException("Use a password with 12 to 128 characters");
        }
        if (!signupDAO.register(email, digest(email, code), Instant.now().getEpochSecond(), name.trim(),
                () -> Passwords.create(password), managerId())) {
            throw new ReimbursementException("That code is incorrect or expired. Request a new code if needed");
        }
    }

    static boolean isThaparEmail(String email) {
        return email != null && email.length() <= 100
                && email.matches("[a-z0-9_+%-]+(\\.[a-z0-9_+%-]+)*@thapar\\.edu");
    }

    private int managerId() throws ReimbursementException {
        return employees.findByEmail(managerEmail)
                .filter(manager -> manager.isUniversity() && manager.getRole() == Role.MANAGER)
                .map(Employee::getId)
                .orElseThrow(() -> new ReimbursementException("Signup is unavailable until a Thapar reviewer is set up"));
    }

    private void checkAvailable() throws ReimbursementException {
        if (!available()) {
            throw new ReimbursementException("Student signup is currently unavailable");
        }
    }

    private String digest(String email, String code) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(hmac.doFinal((email + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Code verification is unavailable", e);
        }
    }

    private void send(String email, String code) throws ReimbursementException {
        try {
            mailer.send(email, code);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ReimbursementException("Could not send the code. Try again in a minute");
        } catch (IOException e) {
            throw new ReimbursementException("Could not send the code. Try again in a minute");
        }
    }
}
