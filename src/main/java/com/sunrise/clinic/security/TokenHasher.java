package com.sunrise.clinic.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class TokenHasher {
    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenHasher() {
    }

    public static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (byte value : hashed) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to protect the sign-in token.", exception);
        }
    }
}
