package com.reimbursement.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.model.Claim;
import com.reimbursement.model.ClaimFactory;
import com.reimbursement.model.Employee;

import jakarta.servlet.http.HttpServletRequest;

public class ClaimForm {

    private final String claimId;
    private final String type;
    private final String amount;
    private final String expenseDate;
    private final String receiptNo;
    private final String description;

    public ClaimForm(String claimId, String type, String amount, String expenseDate, String receiptNo,
            String description) {
        this.claimId = trim(claimId);
        this.type = trim(type);
        this.amount = trim(amount);
        this.expenseDate = trim(expenseDate);
        this.receiptNo = trim(receiptNo);
        this.description = trim(description);
    }

    public static ClaimForm empty() {
        return new ClaimForm("", ClaimFactory.TYPES[0], "", LocalDate.now().toString(), "", "");
    }

    public static ClaimForm from(HttpServletRequest request) {
        return new ClaimForm(request.getParameter("claimId"), request.getParameter("type"),
                request.getParameter("amount"), request.getParameter("expenseDate"),
                request.getParameter("receiptNo"), request.getParameter("description"));
    }

    public static ClaimForm from(Claim claim) {
        return new ClaimForm(String.valueOf(claim.getId()), claim.getType(), claim.getAmount().toPlainString(),
                claim.getExpenseDate().toString(), claim.getReceiptNo(), claim.getDescription());
    }

    public Claim toNewClaim(Employee owner) throws ReimbursementException {
        if (!Arrays.asList(ClaimFactory.TYPES).contains(type)) {
            throw new ReimbursementException("Pick a claim type");
        }
        return ClaimFactory.create(type, owner, toAmount(), toExpenseDate(), receiptNo, description);
    }

    public void copyInto(Claim claim) throws ReimbursementException {
        claim.setAmount(toAmount());
        claim.setExpenseDate(toExpenseDate());
        claim.setReceiptNo(receiptNo);
        claim.setDescription(description);
    }

    public BigDecimal toAmount() throws ReimbursementException {
        if (amount.isEmpty()) {
            throw new ReimbursementException("Enter the amount");
        }
        try {
            return new BigDecimal(amount);
        } catch (NumberFormatException e) {
            throw new ReimbursementException("Amount must be a number, like 1250.50");
        }
    }

    public LocalDate toExpenseDate() throws ReimbursementException {
        if (expenseDate.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(expenseDate);
        } catch (DateTimeParseException e) {
            throw new ReimbursementException("Expense date must look like 2026-11-17");
        }
    }

    public int toClaimId() throws ReimbursementException {
        return parseClaimId(claimId);
    }

    public static int parseClaimId(String text) throws ReimbursementException {
        try {
            return Integer.parseInt(text == null ? "" : text.trim());
        } catch (NumberFormatException e) {
            throw new ReimbursementException("Claim was not found");
        }
    }

    public boolean isEditing() {
        return !claimId.isEmpty();
    }

    public String getClaimId() {
        return claimId;
    }

    public String getType() {
        return type;
    }

    public String getAmount() {
        return amount;
    }

    public String getExpenseDate() {
        return expenseDate;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public String getDescription() {
        return description;
    }

    private static String trim(String text) {
        return text == null ? "" : text.trim();
    }
}
