package com.reimbursement.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.reimbursement.exception.ClaimLimitExceededException;
import com.reimbursement.exception.InvalidClaimStateException;
import com.reimbursement.exception.MissingReceiptException;
import com.reimbursement.exception.ReimbursementException;

public abstract class Claim {

    public static final int MAX_AGE_DAYS = 60;
    public static final int MAX_TEXT_LENGTH = 255;
    public static final int MAX_RECEIPT_LENGTH = 50;

    private int id;
    private Employee employee;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String receiptNo;
    private String description;
    private ClaimStatus status;
    private LocalDateTime submittedAt;
    private String rejectReason;
    private int resubmitCount;
    private boolean proofChecked;

    protected Claim(Employee employee, BigDecimal amount, LocalDate expenseDate, String receiptNo,
            String description) {
        this.employee = employee;
        this.amount = amount;
        this.expenseDate = expenseDate;
        this.receiptNo = receiptNo;
        this.description = description;
        this.status = ClaimStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
        this.resubmitCount = 0;
    }

    public abstract void validate() throws ReimbursementException;

    public abstract String getType();

    public abstract BigDecimal getLimit();

    public boolean needsProof() {
        BigDecimal threshold = getProofAbove();
        return threshold != null && (threshold.signum() == 0
                || (amount != null && amount.compareTo(threshold) > 0));
    }

    public BigDecimal getProofAbove() {
        return BigDecimal.ZERO;
    }

    public ProofKind getReceiptKind() {
        return ProofKind.forClaimType(getType());
    }

    public boolean allowsApprovalLetter() {
        return isStudent() && (getType().equals("TRAVEL") || getType().equals("EVENT"));
    }

    protected boolean isStudent() {
        return employee.getGrade() == Grade.STUDENT;
    }

    protected void checkCommonRules() throws ReimbursementException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReimbursementException("Amount must be more than zero");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new ReimbursementException("Amount can have at most 2 decimal places");
        }
        if (description != null && description.length() > MAX_TEXT_LENGTH) {
            throw new ReimbursementException("Description can be at most " + MAX_TEXT_LENGTH + " characters");
        }
        if (receiptNo != null && receiptNo.length() > MAX_RECEIPT_LENGTH) {
            throw new ReimbursementException("Receipt number can be at most " + MAX_RECEIPT_LENGTH + " characters");
        }
        if (expenseDate == null) {
            throw new ReimbursementException("Expense date is required");
        }
        LocalDate today = LocalDate.now();
        if (expenseDate.isAfter(today)) {
            throw new ReimbursementException("Expense date cannot be in the future");
        }
        if (expenseDate.isBefore(today.minusDays(MAX_AGE_DAYS))) {
            throw new ReimbursementException("Expense date is older than " + MAX_AGE_DAYS + " days");
        }
        if (amount.compareTo(getLimit()) > 0) {
            String who = isStudent() ? "For students, " : "For staff, ";
            throw new ClaimLimitExceededException(who + getTypeName().toLowerCase() + " claims can be at most Rs "
                    + getLimit() + ". This one is Rs " + amount.toPlainString());
        }
    }

    public void checkCanMoveTo(ClaimStatus next) throws InvalidClaimStateException {
        if (!status.canMoveTo(next)) {
            String action = next == ClaimStatus.SUBMITTED ? "resubmitted" : next.name().toLowerCase();
            throw new InvalidClaimStateException("Claim #" + id + " is " + status.name().toLowerCase()
                    + ", so it cannot be " + action);
        }
    }

    public void moveTo(ClaimStatus next) throws InvalidClaimStateException {
        checkCanMoveTo(next);
        status = next;
    }

    protected void requireReceipt(String rule) throws MissingReceiptException {
        if (receiptNo == null || receiptNo.isBlank()) {
            throw new MissingReceiptException(rule);
        }
    }

    public String getTypeName() {
        String type = getType();
        return type.charAt(0) + type.substring(1).toLowerCase();
    }

    public String getFormName() {
        if (isStudent() || employee.isUniversity()) {
            return switch (getType()) {
                case "TRAVEL" -> "Student travel";
                case "ACCOMMODATION" -> "Student accommodation";
                case "EVENT" -> "Academic events and materials";
                default -> getTypeName();
            };
        }
        return switch (getType()) {
            case "TRAVEL" -> "Business travel";
            case "ACCOMMODATION" -> "Business lodging";
            case "FOOD" -> "Business meals";
            case "LOCAL" -> "Local business transport";
            default -> getTypeName();
        };
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public int getResubmitCount() {
        return resubmitCount;
    }

    public void setResubmitCount(int resubmitCount) {
        this.resubmitCount = resubmitCount;
    }

    public boolean isProofChecked() {
        return proofChecked;
    }

    public void setProofChecked(boolean proofChecked) {
        this.proofChecked = proofChecked;
    }

    @Override
    public String toString() {
        return getType() + " claim #" + id + " Rs " + amount + " " + status;
    }
}
