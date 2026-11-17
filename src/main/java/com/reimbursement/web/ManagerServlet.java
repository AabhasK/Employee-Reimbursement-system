package com.reimbursement.web;

import jakarta.servlet.annotation.WebServlet;

@WebServlet("/manager")
public class ManagerServlet extends ApprovalServlet {

    @Override
    protected String page() {
        return "manager";
    }
}
