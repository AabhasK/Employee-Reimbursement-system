package com.reimbursement.web;

import java.io.IOException;
import java.util.UUID;

import com.reimbursement.exception.ReimbursementException;
import com.reimbursement.service.SignupService;
import com.reimbursement.util.AppContext;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/signup")
public class SignupServlet extends HttpServlet {

    private static final String TOKEN = "signupToken";
    private static final String EMAIL = "signupEmail";
    private final SignupService signup = AppContext.getSignupService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (AuthFilter.userOf(request) != null) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }
        show(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (AuthFilter.userOf(request) != null || session == null || session.getAttribute(TOKEN) == null
                || !session.getAttribute(TOKEN).equals(request.getParameter("token"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            if ("verify".equals(request.getParameter("action"))) {
                signup.register((String) session.getAttribute(EMAIL), request.getParameter("code"),
                        request.getParameter("name"), request.getParameter("password"));
                session.removeAttribute(EMAIL);
                session.removeAttribute(TOKEN);
                Views.flash(request, "Student account created. Sign in with your Thapar email.");
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }
            if (!"send".equals(request.getParameter("action"))) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            session.setAttribute(EMAIL, signup.requestCode(request.getParameter("email")));
            Views.flash(request, "Check your inbox for a six-digit code. Wait a minute before requesting another.");
            response.sendRedirect(request.getContextPath() + "/signup");
        } catch (ReimbursementException e) {
            request.setAttribute("error", Messages.describe(e));
            show(request, response);
        }
    }

    private void show(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        if (session.getAttribute(TOKEN) == null) {
            session.setAttribute(TOKEN, UUID.randomUUID().toString());
        }
        request.setAttribute("token", session.getAttribute(TOKEN));
        request.setAttribute("email", session.getAttribute(EMAIL));
        request.setAttribute("signupAvailable", signup.available());
        response.setHeader("Cache-Control", "no-store");
        Views.render("signup", request, response);
    }
}
