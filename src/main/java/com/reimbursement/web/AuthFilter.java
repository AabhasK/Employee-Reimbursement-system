package com.reimbursement.web;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.reimbursement.model.Employee;
import com.reimbursement.model.Role;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebFilter("/*")
public class AuthFilter implements Filter {

    public static final String USER = "user";

    private static final Map<String, Set<Role>> ACCESS = Map.of(
            "/employee", EnumSet.of(Role.EMPLOYEE),
            "/manager", EnumSet.of(Role.MANAGER),
            "/finance", EnumSet.of(Role.FINANCE),
            "/reports", EnumSet.of(Role.MANAGER, Role.FINANCE));

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        request.setCharacterEncoding("UTF-8");

        String path = pathOf(request);
        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }
        Employee user = userOf(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        if (!canOpen(user.getRole(), path)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        // So the back button cannot show a private page after logout
        response.setHeader("Cache-Control", "no-store");
        chain.doFilter(request, response);
    }

    static boolean isPublic(String path) {
        return path.equals("/login") || path.startsWith("/css/") || path.equals("/favicon.ico");
    }

    static boolean canOpen(Role role, String path) {
        for (Map.Entry<String, Set<Role>> entry : ACCESS.entrySet()) {
            String prefix = entry.getKey();
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return entry.getValue().contains(role);
            }
        }
        return true;
    }

    static String homeFor(Role role) {
        return switch (role) {
            case EMPLOYEE -> "/employee";
            case MANAGER -> "/manager";
            case FINANCE -> "/finance";
        };
    }

    static Employee userOf(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (Employee) session.getAttribute(USER);
    }

    private static String pathOf(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        return request.getServletPath() + (pathInfo == null ? "" : pathInfo);
    }
}
