package com.reimbursement.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.reimbursement.model.Role;

class AuthFilterTest {

    @Test
    void eachRoleOpensOnlyItsOwnPage() {
        assertTrue(AuthFilter.canOpen(Role.EMPLOYEE, "/employee"));
        assertFalse(AuthFilter.canOpen(Role.EMPLOYEE, "/manager"));
        assertFalse(AuthFilter.canOpen(Role.EMPLOYEE, "/finance"));
        assertTrue(AuthFilter.canOpen(Role.MANAGER, "/manager"));
        assertFalse(AuthFilter.canOpen(Role.MANAGER, "/finance"));
        assertTrue(AuthFilter.canOpen(Role.FINANCE, "/finance"));
        assertFalse(AuthFilter.canOpen(Role.FINANCE, "/employee"));
    }

    @Test
    void reportsAreForManagerAndFinance() {
        assertTrue(AuthFilter.canOpen(Role.MANAGER, "/reports"));
        assertTrue(AuthFilter.canOpen(Role.FINANCE, "/reports"));
        assertFalse(AuthFilter.canOpen(Role.EMPLOYEE, "/reports"));
    }

    @Test
    void subPathsFollowTheirParentRule() {
        assertFalse(AuthFilter.canOpen(Role.EMPLOYEE, "/finance/reimburse"));
        assertTrue(AuthFilter.canOpen(Role.FINANCE, "/finance/reimburse"));
    }

    @Test
    void sharedPagesAreOpenToEveryLoggedInUser() {
        assertTrue(AuthFilter.canOpen(Role.EMPLOYEE, "/logout"));
        assertTrue(AuthFilter.canOpen(Role.FINANCE, ""));
    }

    @Test
    void onlyLoginAndStylesArePublic() {
        assertTrue(AuthFilter.isPublic("/login"));
        assertTrue(AuthFilter.isPublic("/css/style.css"));
        assertFalse(AuthFilter.isPublic("/employee"));
        assertFalse(AuthFilter.isPublic("/logout"));
    }

    @Test
    void eachRoleHasAHomePage() {
        assertEquals("/employee", AuthFilter.homeFor(Role.EMPLOYEE));
        assertEquals("/manager", AuthFilter.homeFor(Role.MANAGER));
        assertEquals("/finance", AuthFilter.homeFor(Role.FINANCE));
    }
}
