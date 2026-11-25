package com.reimbursement.service;

import java.util.Locale;
import java.util.Optional;

import com.reimbursement.dao.EmployeeDAO;
import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.model.Employee;
import com.reimbursement.util.Passwords;

public class LoginService {

    private final EmployeeDAO employeeDAO;

    public LoginService(EmployeeDAO employeeDAO) {
        this.employeeDAO = employeeDAO;
    }

    public Employee login(String email, String password) throws ReimbursementException {
        if (email == null || email.isBlank() || password == null || password.isEmpty()) {
            throw new ReimbursementException("Enter your email and password");
        }
        Optional<Employee> found = employeeDAO.findByEmail(email.trim().toLowerCase(Locale.ROOT));
        // Same message for both cases so the screen does not reveal which emails exist
        if (found.isEmpty() || !Passwords.matches(password, found.get().getPasswordHash())) {
            throw new ReimbursementException("Wrong email or password");
        }
        return found.get();
    }

    public static String hash(String text) {
        return Passwords.legacyHash(text);
    }
}
