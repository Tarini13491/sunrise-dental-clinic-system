package com.sunrise.clinic.model;

public final class SessionUser {
    private final int userId;
    private final String username;
    private final String fullName;
    private final Role role;

    public SessionUser(int userId, String username, String fullName, Role role) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isStaff() {
        return role == Role.STAFF;
    }
}
