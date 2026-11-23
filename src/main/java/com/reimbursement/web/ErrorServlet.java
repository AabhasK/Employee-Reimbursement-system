package com.reimbursement.web;

import java.io.IOException;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/error")
public class ErrorServlet extends HttpServlet {

    // service() and not doGet(), because a failed POST is forwarded here as a POST
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = code instanceof Integer number ? number : HttpServletResponse.SC_NOT_FOUND;

        Object error = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        if (error instanceof Throwable throwable) {
            log("Unexpected error on " + request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI), throwable);
        }

        response.setStatus(status);
        request.setAttribute("statusCode", status);
        request.setAttribute("errorTitle", titleFor(status));
        request.setAttribute("errorMessage", messageFor(status));
        Views.render("error", request, response);
    }

    static String titleFor(int status) {
        return switch (status) {
            case 403 -> "You cannot open this page";
            case 404 -> "Page not found";
            case 405 -> "This page cannot be opened this way";
            default -> "Something went wrong";
        };
    }

    static String messageFor(int status) {
        return switch (status) {
            case 403 -> "Your role does not have access to this page. Use the links on the left to reach your own pages.";
            case 404 -> "The address may be mistyped, or the page does not exist.";
            case 405 -> "Use the buttons on the page instead of typing this address.";
            default -> "The error has been logged. Try again, and tell the admin if it keeps happening.";
        };
    }
}
