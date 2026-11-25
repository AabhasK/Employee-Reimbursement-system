package com.reimbursement.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Company {

    private static final Properties settings = load();

    private Company() {
    }

    public static String getName() {
        return settings.getProperty("company.name", "Company");
    }

    public static String getLogo() {
        return settings.getProperty("company.logo", "/images/company-logo.png");
    }

    public static String getUniversityName() {
        return settings.getProperty("university.name", "Thapar University");
    }

    public static String getUniversityLogo() {
        return settings.getProperty("university.logo", "/images/thapar-logo.png");
    }

    public static boolean demoEnabled() {
        return !"false".equalsIgnoreCase(System.getenv("APP_COMPANY_DEMO"));
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = Company.class.getResourceAsStream("/company.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            System.err.println("Could not read company.properties: " + e.getMessage());
        }
        return properties;
    }
}
