package com.sunrise.clinic.security;

import com.sunrise.clinic.exception.ForbiddenException;
import com.sunrise.clinic.exception.UnauthorizedException;
import com.sunrise.clinic.model.Role;
import com.sunrise.clinic.model.SessionUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessPolicyTest {
    private final SessionUser admin = new SessionUser(1, "admin", "Clinic Administrator", Role.ADMIN);
    private final SessionUser staff = new SessionUser(2, "nimal", "Nimal Perera", Role.STAFF);

    @Test
    void rejectsMissingSession() {
        assertThrows(UnauthorizedException.class, () -> AccessPolicy.requireUser(null));
    }

    @Test
    void staffCannotUseAdminActions() {
        ForbiddenException error = assertThrows(ForbiddenException.class, () -> AccessPolicy.requireAdmin(staff));
        assertEquals("This action is available to administrators only.", error.getMessage());
        assertEquals(403, error.getStatusCode());
    }

    @Test
    void adminCannotUseStaffOnlyActions() {
        ForbiddenException error = assertThrows(ForbiddenException.class, () -> AccessPolicy.requireStaff(admin));
        assertEquals("This action is available to clinic staff only.", error.getMessage());
    }

    @Test
    void rolePermissionsMatchTheClinicPolicy() {
        assertTrue(AccessPolicy.canManageStaff(Role.ADMIN));
        assertFalse(AccessPolicy.canManageStaff(Role.STAFF));
        assertTrue(AccessPolicy.canManageClinicRecords(Role.STAFF));
        assertFalse(AccessPolicy.canManageClinicRecords(Role.ADMIN));
        assertTrue(AccessPolicy.canViewClinicRecords(Role.ADMIN));
        assertTrue(AccessPolicy.canViewClinicRecords(Role.STAFF));
    }
}
