package com.reimbursement.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.reimbursement.dao.DBConnection;
import com.reimbursement.dao.EmployeeDAO;
import com.reimbursement.exception.DataAccessException;
import com.reimbursement.model.Employee;
import com.reimbursement.model.Grade;
import com.reimbursement.model.Role;

public class JdbcEmployeeDAO implements EmployeeDAO {

    // Many browser requests run at the same time, so the cache must be safe for several threads
    private final Map<Integer, Employee> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<Employee> findById(int id) throws DataAccessException {
        if (cache.containsKey(id)) {
            return Optional.of(cache.get(id));
        }
        return findOne("SELECT * FROM employees WHERE id = ?", id);
    }

    @Override
    public Optional<Employee> findByEmail(String email) throws DataAccessException {
        return findOne("SELECT * FROM employees WHERE email = ?", email);
    }

    @Override
    public List<Employee> findAll() throws DataAccessException {
        List<Employee> employees = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM employees ORDER BY name")) {
            while (rs.next()) {
                employees.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load employees", e);
        }
        return employees;
    }

    private Optional<Employee> findOne(String sql, Object value) throws DataAccessException {
        try (Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load employee", e);
        }
    }

    private Employee mapRow(ResultSet rs) throws SQLException {
        Employee employee = new Employee(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                Role.valueOf(rs.getString("role")),
                rs.getObject("manager_id", Integer.class),
                Grade.valueOf(rs.getString("grade")));
        cache.put(employee.getId(), employee);
        return employee;
    }
}
