package com.sunrise.clinic.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {
    @Test
    void hashesPasswordsAndMatchesTheOriginalValue() {
        String hash = PasswordHasher.hash("Admin#Sunrise26");
        assertNotEquals("Admin#Sunrise26", hash);
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
        assertTrue(PasswordHasher.matches("Admin#Sunrise26", hash));
        assertFalse(PasswordHasher.matches("WrongPass1", hash));
        assertFalse(PasswordHasher.matches("Admin#Sunrise26", null));
    }
}
