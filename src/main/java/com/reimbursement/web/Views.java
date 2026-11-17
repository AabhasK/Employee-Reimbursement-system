package com.reimbursement.web;

import java.io.IOException;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebListener
public class Views implements ServletContextListener {

    private static final String FLASH = "flash";
    private static final String FLASH_ERROR = "flashError";

    private static JakartaServletWebApplication application;
    private static TemplateEngine engine;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        application = JakartaServletWebApplication.buildApplication(event.getServletContext());

        WebApplicationTemplateResolver resolver = new WebApplicationTemplateResolver(application);
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setPrefix("/WEB-INF/templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        // Re-read templates on every request so HTML edits show without a restart
        resolver.setCacheable(false);

        engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
    }

    public static void render(String template, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        WebContext context = new WebContext(application.buildExchange(request, response), request.getLocale());
        context.setVariable("currentUser", AuthFilter.userOf(request));

        HttpSession session = request.getSession(false);
        if (session != null) {
            moveFlash(session, context, FLASH);
            moveFlash(session, context, FLASH_ERROR);
        }
        response.setContentType("text/html;charset=UTF-8");
        engine.process(template, context, response.getWriter());
    }

    // A message kept in the session for one page view, so it survives the redirect after a POST
    public static void flash(HttpServletRequest request, String message) {
        request.getSession().setAttribute(FLASH, message);
    }

    public static void flashError(HttpServletRequest request, String message) {
        request.getSession().setAttribute(FLASH_ERROR, message);
    }

    private static void moveFlash(HttpSession session, WebContext context, String key) {
        Object message = session.getAttribute(key);
        if (message != null) {
            context.setVariable(key, message);
            session.removeAttribute(key);
        }
    }
}
