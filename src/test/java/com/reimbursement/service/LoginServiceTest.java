package com.reimbursement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.reimbursement.dao.EmployeeDAO;
import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.model.Employee;
import com.reimbursement.model.Role;

class LoginServiceTest {

    private final Employee arjun = new Employee(4, "Arjun Singh", "arjun@company.com",
            LoginService.hash("pass123"), Role.EMPLOYEE, 2);

    private final LoginService loginService = new LoginService(new OneEmployeeDAO(arjun));

    @Test
    void hashMatchesMySqlSha2() {
        // Same value MySQL gives for SHA2('pass123', 256) in schema.sql
        assertEquals("9b8769a4a742959a2d0298c36fb70623f2dfacda8436237df08d8dfd5b37374c",
                LoginService.hash("pass123"));
    }

    @Test
    void correctPasswordLogsIn() throws ReimbursementException {
        assertEquals(arjun, loginService.login(" Arjun@Company.com ", "pass123"));
    }

    @Test
    void wrongPasswordIsRejected() {
        assertThrows(ReimbursementException.class, () -> loginService.login("arjun@company.com", "wrong"));
    }

    @Test
    void unknownEmailIsRejected() {
        assertThrows(ReimbursementException.class, () -> loginService.login("nobody@company.com", "pass123"));
    }

    @Test
    void emptyFieldsAreRejected() {
        assertThrows(ReimbursementException.class, () -> loginService.login("", ""));
    }

    private static class OneEmployeeDAO implements EmployeeDAO {

        private final Employee employee;

        OneEmployeeDAO(Employee employee) {
            this.employee = employee;
        }

        @Override
        public Optional<Employee> findById(int id) {
            return id == employee.getId() ? Optional.of(employee) : Optional.empty();
        }

        @Override
        public Optional<Employee> findByEmail(String email) {
            return email.equals(employee.getEmail()) ? Optional.of(employee) : Optional.empty();
        }

        @Override
        public List<Employee> findAll() {
            return new ArrayList<>(List.of(employee));
        }
    }
}
