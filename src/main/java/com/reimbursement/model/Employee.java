package com.reimbursement.model;

import java.io.Serializable;

public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String email;
    private String passwordHash;
    private Role role;
    private Integer managerId;

    public Employee(int id, String name, String email, String passwordHash, Role role, Integer managerId) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.managerId = managerId;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public Integer getManagerId() {
        return managerId;
    }

    @Override
    public String toString() {
        return name + " (" + role + ")";
    }
}
