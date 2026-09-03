package com.sunrise.clinic.security;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordHasher {
    private PasswordHasher() {
    }

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    public static boolean matches(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        return BCrypt.checkpw(plainPassword, storedHash);
    }
}
