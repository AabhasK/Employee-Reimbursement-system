package com.reimbursement.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

// Saves uploaded documents on disk, outside the web folder, under random names
public class ProofStore {

    public static final int MAX_BYTES = 5 * 1024 * 1024;

    private static final Map<String, String> EXTENSIONS = Map.of(
            "application/pdf", ".pdf",
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private final File folder;

    public ProofStore(File folder) {
        this.folder = folder;
    }

    public static boolean isAllowedType(String contentType) {
        return contentType != null && EXTENSIONS.containsKey(contentType);
    }

    // Checks the first bytes of the file, because the browser's content type can be faked
    public static boolean contentMatches(String contentType, byte[] bytes) {
        if (bytes.length < 12) {
            return false;
        }
        return switch (contentType) {
            case "application/pdf" -> bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F';
            case "image/png" -> (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G';
            case "image/jpeg" -> (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
            case "image/webp" -> bytes[0] == 'R' && bytes[1] == 'I' && bytes[8] == 'W' && bytes[9] == 'E';
            default -> false;
        };
    }

    public String save(byte[] bytes, String contentType) throws IOException {
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("Could not create folder " + folder.getAbsolutePath());
        }
        String storedName = UUID.randomUUID() + EXTENSIONS.get(contentType);
        try (FileOutputStream out = new FileOutputStream(new File(folder, storedName))) {
            out.write(bytes);
        }
        return storedName;
    }

    public File fileFor(String storedName) {
        // Only names this class made itself, so "../" tricks cannot reach other files
        if (!storedName.matches("[0-9a-f-]{36}\\.(pdf|jpg|png|webp)")) {
            throw new IllegalArgumentException("Bad stored file name");
        }
        return new File(folder, storedName);
    }
}
