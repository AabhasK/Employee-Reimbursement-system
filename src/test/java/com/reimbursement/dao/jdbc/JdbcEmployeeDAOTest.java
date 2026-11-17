package com.reimbursement.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.reimbursement.dao.EmployeeDAO;
import com.reimbursement.exception.DataAccessException;
import com.reimbursement.model.Employee;
import com.reimbursement.model.Role;

class JdbcEmployeeDAOTest {

    private EmployeeDAO dao;

    @BeforeEach
    void setUp() {
        assumeTrue(new File("db.properties").exists(), "db.properties not found");
        dao = new JdbcEmployeeDAO();
    }

    @Test
    void findsSeedEmployeeByEmail() throws DataAccessException {
        Employee arjun = dao.findByEmail("arjun@company.com").orElseThrow();
        assertEquals("Arjun Singh", arjun.getName());
        assertEquals(Role.EMPLOYEE, arjun.getRole());
        assertEquals(2, arjun.getManagerId());
    }

    @Test
    void financeUserHasNoManager() throws DataAccessException {
        Employee neha = dao.findById(1).orElseThrow();
        assertEquals(Role.FINANCE, neha.getRole());
        assertNull(neha.getManagerId());
    }

    @Test
    void unknownEmailGivesEmptyOptional() throws DataAccessException {
        Optional<Employee> nobody = dao.findByEmail("nobody@company.com");
        assertTrue(nobody.isEmpty());
    }

    @Test
    void secondLookupComesFromCache() throws DataAccessException {
        Employee first = dao.findById(4).orElseThrow();
        Employee second = dao.findById(4).orElseThrow();
        assertSame(first, second);
    }

    @Test
    void findAllReturnsEverySeedUser() throws DataAccessException {
        assertEquals(7, dao.findAll().size());
    }
}
