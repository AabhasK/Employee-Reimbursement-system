package com.reimbursement.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.reimbursement.dao.AuditDAO;
import com.reimbursement.dao.DBConnection;
import com.reimbursement.exception.DataAccessException;
import com.reimbursement.model.AuditEntry;
import com.reimbursement.model.ClaimStatus;

public class JdbcAuditDAO implements AuditDAO {

    @Override
    public void save(AuditEntry entry) throws DataAccessException {
        String sql = "INSERT INTO claim_audit (claim_id, actor_id, from_status, to_status, note, at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, entry.getClaimId());
            ps.setInt(2, entry.getActorId());
            ps.setString(3, entry.getFromStatus() == null ? null : entry.getFromStatus().name());
            ps.setString(4, entry.getToStatus().name());
            ps.setString(5, entry.getNote());
            ps.setTimestamp(6, Timestamp.valueOf(entry.getAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not save audit entry for claim " + entry.getClaimId(), e);
        }
    }

    @Override
    public List<AuditEntry> findByClaim(int claimId) throws DataAccessException {
        List<AuditEntry> entries = new ArrayList<>();
        String sql = "SELECT * FROM claim_audit WHERE claim_id = ? ORDER BY id";
        try (Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, claimId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load audit for claim " + claimId, e);
        }
        return entries;
    }

    private AuditEntry mapRow(ResultSet rs) throws SQLException {
        String from = rs.getString("from_status");
        AuditEntry entry = new AuditEntry(
                rs.getInt("claim_id"),
                rs.getInt("actor_id"),
                from == null ? null : ClaimStatus.valueOf(from),
                ClaimStatus.valueOf(rs.getString("to_status")),
                rs.getString("note"));
        entry.setId(rs.getInt("id"));
        entry.setAt(rs.getTimestamp("at").toLocalDateTime());
        return entry;
    }
}
