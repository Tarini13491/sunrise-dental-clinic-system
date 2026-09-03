package com.sunrise.clinic.service;

import com.sunrise.clinic.dao.AuthTokenDao;
import com.sunrise.clinic.dao.UserDao;
import com.sunrise.clinic.exception.ForbiddenException;
import com.sunrise.clinic.exception.UnauthorizedException;
import com.sunrise.clinic.model.AccountStatus;
import com.sunrise.clinic.model.AuthToken;
import com.sunrise.clinic.model.SessionUser;
import com.sunrise.clinic.model.UserAccount;
import com.sunrise.clinic.security.PasswordHasher;
import com.sunrise.clinic.security.TokenHasher;
import com.sunrise.clinic.validation.FieldValidator;

import java.time.LocalDateTime;
import java.util.Optional;

public class AuthService {
    private static final int REMEMBER_DAYS = 30;
    private final UserDao users;
    private final AuthTokenDao tokens;

    public AuthService(UserDao users, AuthTokenDao tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    public SessionUser login(String username, String password) {
        String safeUsername = FieldValidator.required(username, "Username");
        String safePassword = FieldValidator.required(password, "Password");
        UserAccount account = users.findByUsername(safeUsername)
                .orElseThrow(() -> new UnauthorizedException("The username or password is incorrect."));
        if (account.getStatus() == AccountStatus.BLOCKED) {
            throw new ForbiddenException("This account has been blocked. Contact an administrator.");
        }
        if (!PasswordHasher.matches(safePassword, account.getPasswordHash())) {
            throw new UnauthorizedException("The username or password is incorrect.");
        }
        return toSession(account);
    }

    public String createPersistentToken(int userId) {
        String rawToken = TokenHasher.randomToken();
        AuthToken token = new AuthToken();
        token.setUserId(userId);
        token.setTokenHash(TokenHasher.hash(rawToken));
        token.setExpiresAt(LocalDateTime.now().plusDays(REMEMBER_DAYS));
        tokens.insert(token);
        return rawToken;
    }

    public Optional<SessionUser> restorePersistentLogin(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        String hash = TokenHasher.hash(rawToken);
        AuthToken stored = tokens.findByHash(hash).orElse(null);
        if (stored == null) {
            return Optional.empty();
        }
        if (stored.isExpired()) {
            tokens.deleteByHash(hash);
            return Optional.empty();
        }
        UserAccount account = users.findById(stored.getUserId()).orElse(null);
        if (account == null || account.getStatus() == AccountStatus.BLOCKED) {
            tokens.deleteByHash(hash);
            return Optional.empty();
        }
        return Optional.of(toSession(account));
    }

    public void revokePersistentLogin(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        tokens.deleteByHash(TokenHasher.hash(rawToken));
    }

    public void revokeAllPersistentLogins(int userId) {
        tokens.deleteByUserId(userId);
    }

    private SessionUser toSession(UserAccount account) {
        return new SessionUser(account.getUserId(), account.getUsername(), account.getFullName(), account.getRole());
    }
}
