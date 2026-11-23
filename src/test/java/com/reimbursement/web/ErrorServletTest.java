package com.reimbursement.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ErrorServletTest {

    @Test
    void forbiddenPageExplainsTheRoleRule() {
        assertEquals("You cannot open this page", ErrorServlet.titleFor(403));
        assertTrue(ErrorServlet.messageFor(403).contains("role"));
    }

    @Test
    void missingPageHasItsOwnTitle() {
        assertEquals("Page not found", ErrorServlet.titleFor(404));
    }

    @Test
    void serverErrorsShareOneMessage() {
        assertEquals("Something went wrong", ErrorServlet.titleFor(500));
        assertEquals(ErrorServlet.messageFor(500), ErrorServlet.messageFor(503));
    }
}
