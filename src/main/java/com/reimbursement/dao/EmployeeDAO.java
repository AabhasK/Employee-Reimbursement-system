package com.reimbursement.dao;

import java.util.List;
import java.util.Optional;

import com.reimbursement.exception.DataAccessException;
import com.reimbursement.model.Employee;

public interface EmployeeDAO {

    Optional<Employee> findById(int id) throws DataAccessException;

    Optional<Employee> findByEmail(String email) throws DataAccessException;

    List<Employee> findAll() throws DataAccessException;
}
