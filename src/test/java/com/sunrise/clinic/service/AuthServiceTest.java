package com.sunrise.clinic.service;

import com.sunrise.clinic.exception.ForbiddenException;
import com.sunrise.clinic.exception.UnauthorizedException;
import com.sunrise.clinic.exception.ValidationException;
import com.sunrise.clinic.model.AccountStatus;
import com.sunrise.clinic.model.Role;
import com.sunrise.clinic.model.SessionUser;
import com.sunrise.clinic.model.UserAccount;
import com.sunrise.clinic.security.PasswordHasher;
import com.sunrise.clinic.support.InMemoryAuthTokenDao;
import com.sunrise.clinic.support.InMemoryUserDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {
    private InMemoryUserDao users;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserDao();
        authService = new AuthService(users, new InMemoryAuthTokenDao());
        UserAccount admin = new UserAccount();
        admin.setUsername("admin");
        admin.setPasswordHash(PasswordHasher.hash("Admin#Sunrise26"));
        admin.setFullName("Clinic Administrator");
        admin.setEmail("admin@sunrisedental.lk");
        admin.setContactNumber("0112345678");
        admin.setRole(Role.ADMIN);
        admin.setStatus(AccountStatus.ACTIVE);
        users.insert(admin);
        UserAccount staff = new UserAccount();
        staff.setUsername("nimal");
        staff.setPasswordHash(PasswordHasher.hash("Staff1234"));
        staff.setFullName("Nimal Perera");
        staff.setEmail("nimal@sunrisedental.lk");
        staff.setContactNumber("0771234567");
        staff.setRole(Role.STAFF);
        staff.setStatus(AccountStatus.ACTIVE);
        users.insert(staff);
    }

    @Test
    void authenticatesValidCredentialsAndReturnsTheRole() {
        SessionUser user = authService.login("admin", "Admin#Sunrise26");
        assertEquals(Role.ADMIN, user.getRole());
        assertEquals("admin", user.getUsername());
    }

    @Test
    void rejectsEmptyLoginFields() {
        assertThrows(ValidationException.class, () -> authService.login(" ", "Admin#Sunrise26"));
        assertThrows(ValidationException.class, () -> authService.login("admin", " "));
    }

    @Test
    void rejectsUnknownOrIncorrectPasswordsWithTheSameMessage() {
        UnauthorizedException unknown = assertThrows(UnauthorizedException.class, () -> authService.login("missing", "Admin#Sunrise26"));
        UnauthorizedException wrong = assertThrows(UnauthorizedException.class, () -> authService.login("admin", "WrongPass1"));
        assertEquals("The username or password is incorrect.", unknown.getMessage());
        assertEquals("The username or password is incorrect.", wrong.getMessage());
        assertEquals(401, wrong.getStatusCode());
    }

    @Test
    void blockedStaffCannotLogIn() {
        users.updateStatus(2, AccountStatus.BLOCKED);
        ForbiddenException error = assertThrows(ForbiddenException.class, () -> authService.login("nimal", "Staff1234"));
        assertEquals("This account has been blocked. Contact an administrator.", error.getMessage());
    }

    @Test
    void persistentLoginUsesAHashedTokenAndSurvivesWithoutThePassword() {
        SessionUser user = authService.login("nimal", "Staff1234");
        String token = authService.createPersistentToken(user.getUserId());
        SessionUser restored = authService.restorePersistentLogin(token).orElseThrow();
        assertEquals("nimal", restored.getUsername());
        assertEquals(Role.STAFF, restored.getRole());
        authService.revokePersistentLogin(token);
        assertTrue(authService.restorePersistentLogin(token).isEmpty());
    }

    @Test
    void blockedAccountsCannotBeRestoredFromAPersistentToken() {
        SessionUser user = authService.login("nimal", "Staff1234");
        String token = authService.createPersistentToken(user.getUserId());
        users.updateStatus(user.getUserId(), AccountStatus.BLOCKED);
        assertTrue(authService.restorePersistentLogin(token).isEmpty());
    }

    @Test
    void persistentTokenIsNotThePassword() {
        SessionUser user = authService.login("admin", "Admin#Sunrise26");
        String token = authService.createPersistentToken(user.getUserId());
        assertFalse(token.contains("Admin#Sunrise26"));
        assertTrue(token.length() >= 32);
    }
}
