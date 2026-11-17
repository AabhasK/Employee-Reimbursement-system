package com.reimbursement.dao.jdbc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.reimbursement.dao.ClaimDAO;
import com.reimbursement.dao.DBConnection;
import com.reimbursement.dao.EmployeeDAO;
import com.reimbursement.exception.DataAccessException;
import com.reimbursement.model.Claim;
import com.reimbursement.model.ClaimFactory;
import com.reimbursement.model.ClaimStatus;
import com.reimbursement.model.Employee;

public class JdbcClaimDAO implements ClaimDAO {

    private static final String INSERT_SQL = "INSERT INTO claims (employee_id, type, amount, expense_date, "
            + "receipt_no, description, status, reject_reason, resubmit_count, submitted_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL = "UPDATE claims SET amount = ?, expense_date = ?, receipt_no = ?, "
            + "description = ?, status = ?, reject_reason = ?, resubmit_count = ? WHERE id = ?";

    private final EmployeeDAO employeeDAO;

    public JdbcClaimDAO(EmployeeDAO employeeDAO) {
        this.employeeDAO = employeeDAO;
    }

    @Override
    public void save(Claim claim) throws DataAccessException {
        try (Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, claim.getEmployee().getId());
            ps.setString(2, claim.getType());
            ps.setBigDecimal(3, claim.getAmount());
            ps.setDate(4, Date.valueOf(claim.getExpenseDate()));
            ps.setString(5, claim.getReceiptNo());
            ps.setString(6, claim.getDescription());
            ps.setString(7, claim.getStatus().name());
            ps.setString(8, claim.getRejectReason());
            ps.setInt(9, claim.getResubmitCount());
            ps.setTimestamp(10, Timestamp.valueOf(claim.getSubmittedAt()));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    claim.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save claim", e);
        }
    }

    @Override
    public void update(Claim claim) throws DataAccessException {
        int rows;
        try (Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(UPDATE_SQL)) {
            ps.setBigDecimal(1, claim.getAmount());
            ps.setDate(2, Date.valueOf(claim.getExpenseDate()));
            ps.setString(3, claim.getReceiptNo());
            ps.setString(4, claim.getDescription());
            ps.setString(5, claim.getStatus().name());
            ps.setString(6, claim.getRejectReason());
            ps.setInt(7, claim.getResubmitCount());
            ps.setInt(8, claim.getId());
            rows = ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update claim " + claim.getId(), e);
        }
        if (rows == 0) {
            throw new DataAccessException("No claim with id " + claim.getId());
        }
    }

    @Override
    public Optional<Claim> findById(int id) throws DataAccessException {
        List<Claim> claims = query("SELECT * FROM claims WHERE id = ?", id);
        if (claims.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(claims.get(0));
    }

    @Override
    public List<Claim> findByEmployee(int employeeId) throws DataAccessException {
        return query("SELECT * FROM claims WHERE employee_id = ? ORDER BY submitted_at DESC", employeeId);
    }

    @Override
    public List<Claim> findByStatus(ClaimStatus status) throws DataAccessException {
        return query("SELECT * FROM claims WHERE status = ? ORDER BY submitted_at", status.name());
    }

    @Override
    public List<Claim> findAll() throws DataAccessException {
        return query("SELECT * FROM claims ORDER BY submitted_at DESC");
    }

    @Override
    public boolean isReceiptUsed(String receiptNo, int ignoreClaimId) throws DataAccessException {
        String sql = "SELECT COUNT(*) FROM claims WHERE receipt_no = ? AND id <> ?";
        try (Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, receiptNo);
            ps.setInt(2, ignoreClaimId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check receipt " + receiptNo, e);
        }
    }

    private List<Claim> query(String sql, Object... params) throws DataAccessException {
        List<Claim> claims = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    claims.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load claims", e);
        }
        return claims;
    }

    private Claim mapRow(ResultSet rs) throws SQLException, DataAccessException {
        int employeeId = rs.getInt("employee_id");
        Employee employee = employeeDAO.findById(employeeId)
                .orElseThrow(() -> new DataAccessException("No employee with id " + employeeId));

        String type = rs.getString("type");
        BigDecimal amount = rs.getBigDecimal("amount");
        LocalDate expenseDate = rs.getDate("expense_date").toLocalDate();
        String receiptNo = rs.getString("receipt_no");
        String description = rs.getString("description");

        Claim claim = ClaimFactory.create(type, employee, amount, expenseDate, receiptNo, description);
        claim.setId(rs.getInt("id"));
        claim.setStatus(ClaimStatus.valueOf(rs.getString("status")));
        claim.setRejectReason(rs.getString("reject_reason"));
        claim.setResubmitCount(rs.getInt("resubmit_count"));
        claim.setSubmittedAt(rs.getTimestamp("submitted_at").toLocalDateTime());
        return claim;
    }
}
