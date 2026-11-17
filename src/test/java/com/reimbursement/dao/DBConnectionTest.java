package com.reimbursement.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;

class DBConnectionTest {

    @Test
    void connectsAndFindsSeedEmployees() throws SQLException {
        // Skip on machines that have no database set up yet
        assumeTrue(new File("db.properties").exists(), "db.properties not found");

        try (Connection con = DBConnection.getConnection();
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM employees")) {
            assertTrue(con.isValid(2));
            assertTrue(rs.next());
            assertEquals(7, rs.getInt(1));
        }
    }
}
