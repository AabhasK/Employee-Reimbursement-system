package com.reimbursement.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.reimbursement.dao.DBConnection;
import com.reimbursement.dao.ProofDAO;
import com.reimbursement.exception.DataAccessException;
import com.reimbursement.model.ProofDocument;
import com.reimbursement.model.ProofKind;

public class JdbcProofDAO implements ProofDAO {

    @Override
    public void save(ProofDocument proof) throws DataAccessException {
        String sql = "INSERT INTO claim_proofs (claim_id, kind, original_name, stored_name, content_type, "
                + "size_bytes, uploaded_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, proof.getClaimId());
            ps.setString(2, proof.getKind().name());
            ps.setString(3, proof.getOriginalName());
            ps.setString(4, proof.getStoredName());
            ps.setString(5, proof.getContentType());
            ps.setInt(6, proof.getSizeBytes());
            ps.setTimestamp(7, Timestamp.valueOf(proof.getUploadedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    proof.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the document for claim " + proof.getClaimId(), e);
        }
    }

    @Override
    public Optional<ProofDocument> findById(int id) throws DataAccessException {
        List<ProofDocument> proofs = query("SELECT * FROM claim_proofs WHERE id = ?", id);
        if (proofs.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(proofs.get(0));
    }

    @Override
    public List<ProofDocument> findByClaim(int claimId) throws DataAccessException {
        return query("SELECT * FROM claim_proofs WHERE claim_id = ? ORDER BY id", claimId);
    }

    private List<ProofDocument> query(String sql, int value) throws DataAccessException {
        List<ProofDocument> proofs = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    proofs.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load documents", e);
        }
        return proofs;
    }

    private ProofDocument mapRow(ResultSet rs) throws SQLException {
        ProofDocument proof = new ProofDocument(
                rs.getInt("claim_id"),
                ProofKind.parse(rs.getString("kind")),
                rs.getString("original_name"),
                rs.getString("stored_name"),
                rs.getString("content_type"),
                rs.getInt("size_bytes"));
        proof.setId(rs.getInt("id"));
        proof.setUploadedAt(rs.getTimestamp("uploaded_at").toLocalDateTime());
        return proof;
    }
}
