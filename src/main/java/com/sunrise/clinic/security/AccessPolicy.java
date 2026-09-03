package com.sunrise.clinic.security;

import com.sunrise.clinic.exception.ForbiddenException;
import com.sunrise.clinic.exception.UnauthorizedException;
import com.sunrise.clinic.model.Role;
import com.sunrise.clinic.model.SessionUser;

public final class AccessPolicy {
    private AccessPolicy() {
    }

    public static SessionUser requireUser(SessionUser user) {
        if (user == null) {
            throw new UnauthorizedException("Please log in to continue.");
        }
        return user;
    }

    public static SessionUser requireAdmin(SessionUser user) {
        requireUser(user);
        if (!user.isAdmin()) {
            throw new ForbiddenException("This action is available to administrators only.");
        }
        return user;
    }

    public static SessionUser requireStaff(SessionUser user) {
        requireUser(user);
        if (!user.isStaff()) {
            throw new ForbiddenException("This action is available to clinic staff only.");
        }
        return user;
    }

    public static boolean canManageStaff(Role role) {
        return role == Role.ADMIN;
    }

    public static boolean canManageClinicRecords(Role role) {
        return role == Role.STAFF;
    }

    public static boolean canViewClinicRecords(Role role) {
        return role == Role.ADMIN || role == Role.STAFF;
    }
}
