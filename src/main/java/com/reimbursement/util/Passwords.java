package com.reimbursement.util;

import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HexFormat;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Passwords {

    private static final String PREFIX = "pbkdf2-sha256$";
    private static final int ITERATIONS = 600_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private Passwords() {
    }

    public static boolean validLength(String password) {
        return password != null && password.length() >= 12 && password.length() <= 128;
    }

    public static String create(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return PREFIX + HexFormat.of().formatHex(salt) + "$" + derive(password, salt);
    }

    public static boolean matches(String password, String saved) {
        if (password == null || password.length() > 128) {
            return false;
        }
        if (isModern(saved)) {
            String[] parts = saved.split("\\$", -1);
            if (parts.length != 3 || !parts[1].matches("[0-9a-f]{32}")) {
                return false;
            }
            return equal(parts[2], derive(password, HexFormat.of().parseHex(parts[1])));
        }
        return equal(saved, legacyHash(password));
    }

    public static boolean isModern(String hash) {
        return hash != null && hash.startsWith(PREFIX);
    }

    public static boolean equal(String first, String second) {
        return MessageDigest.isEqual(first.getBytes(StandardCharsets.UTF_8), second.getBytes(StandardCharsets.UTF_8));
    }

    public static String legacyHash(String password) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static String derive(String password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);
        try {
            byte[] result = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return HexFormat.of().formatHex(result);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing is unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }

    public static void main(String[] args) {
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("Run from an interactive terminal");
        }
        char[] chars = console.readPassword("New account password: ");
        if (chars == null) {
            return;
        }
        try {
            String password = new String(chars);
            if (!validLength(password)) {
                throw new IllegalArgumentException("Use 12 to 128 characters");
            }
            console.printf("%s%n", create(password));
        } finally {
            Arrays.fill(chars, '\0');
        }
    }
}
