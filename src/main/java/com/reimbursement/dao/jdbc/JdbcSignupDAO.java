package com.reimbursement.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;

import com.reimbursement.dao.DBConnection;
import com.reimbursement.exception.DataAccessException;
import com.reimbursement.util.Passwords;

public class JdbcSignupDAO {

    public boolean issue(String email, String hash, long now) throws DataAccessException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement seed = con.prepareStatement("INSERT INTO signup_codes (email, code_hash, issued_at) "
                    + "VALUES (?, '', 0) ON DUPLICATE KEY UPDATE email = email");
                    PreparedStatement update = con.prepareStatement("UPDATE signup_codes SET code_hash = ?, "
                            + "issued_at = ?, attempts = 0 WHERE email = ? AND issued_at <= ?")) {
                seed.setString(1, email);
                seed.executeUpdate();
                update.setString(1, hash);
                update.setLong(2, now);
                update.setString(3, email);
                update.setLong(4, now - 60);
                boolean issued = update.executeUpdate() == 1;
                con.commit();
                return issued;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not request a signup code", e);
        }
    }

    public boolean register(String email, String codeHash, long now, String name, Supplier<String> passwordHash,
            int managerId) throws DataAccessException {
        String select = "SELECT code_hash, issued_at, attempts FROM signup_codes WHERE email = ? FOR UPDATE";
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement query = con.prepareStatement(select)) {
                query.setString(1, email);
                boolean verified;
                try (ResultSet row = query.executeQuery()) {
                    if (!row.next() || row.getLong("issued_at") <= now - 600 || row.getInt("attempts") >= 5) {
                        con.commit();
                        return false;
                    }
                    verified = Passwords.equal(row.getString("code_hash"), codeHash);
                }
                if (verified) {
                    saveStudent(con, email, name, passwordHash.get(), managerId);
                    updateForEmail(con, "DELETE FROM signup_codes WHERE email = ?", email);
                } else {
                    updateForEmail(con, "UPDATE signup_codes SET attempts = attempts + 1 WHERE email = ?", email);
                }
                con.commit();
                return verified;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not create the student account", e);
        }
    }

    private void saveStudent(Connection con, String email, String name, String password, int managerId)
            throws SQLException {
        String sql = "INSERT INTO employees (email, name, password_hash, manager_id, role, grade) "
                + "VALUES (?, ?, ?, ?, 'EMPLOYEE', 'STUDENT')";
        try (PreparedStatement insert = con.prepareStatement(sql)) {
            insert.setString(1, email);
            insert.setString(2, name);
            insert.setString(3, password);
            insert.setInt(4, managerId);
            insert.executeUpdate();
        }
    }

    private void updateForEmail(Connection con, String sql, String email) throws SQLException {
        try (PreparedStatement update = con.prepareStatement(sql)) {
            update.setString(1, email);
            update.executeUpdate();
        }
    }
}
