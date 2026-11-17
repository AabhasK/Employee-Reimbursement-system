package com.reimbursement.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

import com.reimbursement.dao.EmployeeDAO;
import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.model.Employee;

public class LoginService {

    private final EmployeeDAO employeeDAO;

    public LoginService(EmployeeDAO employeeDAO) {
        this.employeeDAO = employeeDAO;
    }

    public Employee login(String email, String password) throws ReimbursementException {
        if (email == null || email.isBlank() || password == null || password.isEmpty()) {
            throw new ReimbursementException("Enter your email and password");
        }
        Optional<Employee> found = employeeDAO.findByEmail(email.trim().toLowerCase());
        // Same message for both cases so the screen does not reveal which emails exist
        if (found.isEmpty() || !found.get().getPasswordHash().equals(hash(password))) {
            throw new ReimbursementException("Wrong email or password");
        }
        return found.get();
    }

    public static String hash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
