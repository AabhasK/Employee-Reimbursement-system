package com.reimbursement.model;

import java.time.LocalDateTime;

public class AuditEntry {

    private int id;
    private int claimId;
    private int actorId;
    private ClaimStatus fromStatus;
    private ClaimStatus toStatus;
    private String note;
    private LocalDateTime at;

    public AuditEntry(int claimId, int actorId, ClaimStatus fromStatus, ClaimStatus toStatus, String note) {
        this.claimId = claimId;
        this.actorId = actorId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.note = note;
        this.at = LocalDateTime.now();
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

    public int getActorId() {
        return actorId;
    }

    public ClaimStatus getFromStatus() {
        return fromStatus;
    }

    public ClaimStatus getToStatus() {
        return toStatus;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getAt() {
        return at;
    }

    public void setAt(LocalDateTime at) {
        this.at = at;
    }
}
