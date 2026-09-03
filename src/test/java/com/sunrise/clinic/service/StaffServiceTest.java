package com.sunrise.clinic.service;

import com.sunrise.clinic.exception.ConflictException;
import com.sunrise.clinic.exception.ForbiddenException;
import com.sunrise.clinic.model.AccountStatus;
import com.sunrise.clinic.model.Role;
import com.sunrise.clinic.model.SessionUser;
import com.sunrise.clinic.model.UserAccount;
import com.sunrise.clinic.support.InMemoryAuthTokenDao;
import com.sunrise.clinic.support.InMemoryUserDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StaffServiceTest {
    private InMemoryUserDao users;
    private StaffService staffService;
    private final SessionUser admin = new SessionUser(1, "admin", "Clinic Administrator", Role.ADMIN);
    private final SessionUser staffActor = new SessionUser(2, "nimal", "Nimal Perera", Role.STAFF);

    @BeforeEach
    void setUp() {
        users = new InMemoryUserDao();
        staffService = new StaffService(users, new InMemoryAuthTokenDao());
        UserAccount adminAccount = new UserAccount();
        adminAccount.setUsername("admin");
        adminAccount.setPasswordHash("hash");
        adminAccount.setFullName("Clinic Administrator");
        adminAccount.setEmail("admin@sunrisedental.lk");
        adminAccount.setContactNumber("0112345678");
        adminAccount.setRole(Role.ADMIN);
        adminAccount.setStatus(AccountStatus.ACTIVE);
        users.insert(adminAccount);
    }

    @Test
    void staffCannotRegisterOtherStaff() {
        assertThrows(ForbiddenException.class, () -> staffService.register(
                staffActor, "saman", "Staff1234", "Saman Silva", "saman@sunrisedental.lk", "0771112233"));
    }

    @Test
    void adminCreatesStaffAccountsAndIgnoresAnyRequestedAdminRole() {
        UserAccount created = staffService.register(admin, "saman", "Staff1234", "Saman Silva", "saman@sunrisedental.lk", "0771112233");
        assertEquals(Role.STAFF, created.getRole());
        assertEquals(AccountStatus.ACTIVE, created.getStatus());
        assertNull(created.getPasswordHash());
        assertEquals(Role.STAFF, users.findByUsername("saman").orElseThrow().getRole());
    }

    @Test
    void rejectsDuplicateUsernames() {
        staffService.register(admin, "saman", "Staff1234", "Saman Silva", "saman@sunrisedental.lk", "0771112233");
        assertThrows(ConflictException.class, () -> staffService.register(
                admin, "saman", "Staff1234", "Saman Silva", "other@sunrisedental.lk", "0771112244"));
    }

    @Test
    void adminCanBlockAndReactivateStaff() {
        UserAccount created = staffService.register(admin, "saman", "Staff1234", "Saman Silva", "saman@sunrisedental.lk", "0771112233");
        UserAccount blocked = staffService.changeStatus(admin, created.getUserId(), AccountStatus.BLOCKED);
        assertEquals(AccountStatus.BLOCKED, blocked.getStatus());
        UserAccount active = staffService.changeStatus(admin, created.getUserId(), AccountStatus.ACTIVE);
        assertEquals(AccountStatus.ACTIVE, active.getStatus());
    }

    @Test
    void cannotBlockAdministratorAccountsThroughStaffManagement() {
        assertThrows(ForbiddenException.class, () -> staffService.changeStatus(admin, 1, AccountStatus.BLOCKED));
    }
}
