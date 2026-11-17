package com.reimbursement.dao;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {

    private static final String CONFIG_FILE = "db.properties";

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        Properties settings = loadSettings();
        String url = settings.getProperty("db.url");
        String user = settings.getProperty("db.user");
        String password = settings.getProperty("db.password");
        return DriverManager.getConnection(url, user, password);
    }

    private static Properties loadSettings() throws SQLException {
        Properties settings = new Properties();
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            settings.load(reader);
        } catch (IOException e) {
            throw new SQLException("Could not read " + CONFIG_FILE + ". Copy db.properties.example and fill it in.", e);
        }
        return settings;
    }
}
