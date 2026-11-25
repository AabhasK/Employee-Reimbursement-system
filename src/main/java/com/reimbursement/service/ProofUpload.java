package com.reimbursement.service;

import com.reimbursement.model.ProofKind;

// One file picked in the claim form, before it is saved
public class ProofUpload {

    private final ProofKind kind;
    private final String fileName;
    private final String contentType;
    private final byte[] bytes;

    public ProofUpload(ProofKind kind, String fileName, String contentType, byte[] bytes) {
        this.kind = kind;
        this.fileName = fileName;
        this.contentType = contentType;
        this.bytes = bytes;
    }

    public ProofKind getKind() {
        return kind;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
