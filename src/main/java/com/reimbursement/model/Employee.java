package com.reimbursement.model;

import java.io.Serializable;
import java.util.Locale;

public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String email;
    private String passwordHash;
    private Role role;
    private Integer managerId;
    private Grade grade;

    public Employee(int id, String name, String email, String passwordHash, Role role, Integer managerId) {
        this(id, name, email, passwordHash, role, managerId, Grade.STAFF);
    }

    public Employee(int id, String name, String email, String passwordHash, Role role, Integer managerId,
            Grade grade) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.managerId = managerId;
        this.grade = grade;
    }

    public Grade getGrade() {
        return grade;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public boolean isUniversity() {
        return email != null && email.toLowerCase(Locale.ROOT).endsWith("@thapar.edu");
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public Integer getManagerId() {
        return managerId;
    }

    public String getInitials() {
        String[] parts = name.trim().split("\\s+");
        String initials = parts[0].substring(0, 1);
        if (parts.length > 1) {
            initials += parts[parts.length - 1].substring(0, 1);
        }
        return initials.toUpperCase();
    }

    @Override
    public String toString() {
        return name + " (" + role + ")";
    }
}
