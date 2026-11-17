package com.reimbursement.web;

import java.io.IOException;

import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.model.Employee;
import com.reimbursement.service.LoginService;
import com.reimbursement.util.AppContext;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final LoginService loginService = AppContext.getLoginService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Employee user = AuthFilter.userOf(request);
        if (user != null) {
            response.sendRedirect(request.getContextPath() + AuthFilter.homeFor(user.getRole()));
            return;
        }
        Views.render("login", request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String email = request.getParameter("email");
        try {
            Employee user = loginService.login(email, request.getParameter("password"));

            // Fresh session after login, so a session id seen before login is useless
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            request.getSession(true).setAttribute(AuthFilter.USER, user);
            response.sendRedirect(request.getContextPath() + AuthFilter.homeFor(user.getRole()));
        } catch (ReimbursementException e) {
            request.setAttribute("error", Messages.describe(e));
            request.setAttribute("email", email);
            Views.render("login", request, response);
        }
    }
}
