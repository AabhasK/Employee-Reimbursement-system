package com.reimbursement.model;

import java.time.LocalDateTime;

public class ProofDocument {

    private int id;
    private int claimId;
    private ProofKind kind;
    private String originalName;
    private String storedName;
    private String contentType;
    private int sizeBytes;
    private LocalDateTime uploadedAt;

    public ProofDocument(int claimId, ProofKind kind, String originalName, String storedName, String contentType,
            int sizeBytes) {
        this.claimId = claimId;
        this.kind = kind;
        this.originalName = originalName;
        this.storedName = storedName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.uploadedAt = LocalDateTime.now();
    }

    public boolean isImage() {
        return contentType.startsWith("image/");
    }

    public String getSizeLabel() {
        if (sizeBytes < 1024 * 1024) {
            return Math.max(1, sizeBytes / 1024) + " KB";
        }
        return String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0));
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getClaimId() {
        return claimId;
    }

    public ProofKind getKind() {
        return kind;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getStoredName() {
        return storedName;
    }

    public String getContentType() {
        return contentType;
    }

    public int getSizeBytes() {
        return sizeBytes;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
