package com.sunrise.clinic.service;

import com.sunrise.clinic.dao.AuthTokenDao;
import com.sunrise.clinic.dao.UserDao;
import com.sunrise.clinic.exception.ConflictException;
import com.sunrise.clinic.exception.ForbiddenException;
import com.sunrise.clinic.exception.NotFoundException;
import com.sunrise.clinic.exception.ValidationException;
import com.sunrise.clinic.model.AccountStatus;
import com.sunrise.clinic.model.Role;
import com.sunrise.clinic.model.SessionUser;
import com.sunrise.clinic.model.UserAccount;
import com.sunrise.clinic.security.AccessPolicy;
import com.sunrise.clinic.security.PasswordHasher;
import com.sunrise.clinic.validation.FieldValidator;

import java.util.List;
import java.util.stream.Collectors;

public class StaffService {
    private final UserDao users;
    private final AuthTokenDao tokens;

    public StaffService(UserDao users, AuthTokenDao tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    public List<UserAccount> list(SessionUser actor) {
        AccessPolicy.requireAdmin(actor);
        return users.findStaffMembers().stream().map(this::publicCopy).collect(Collectors.toList());
    }

    public UserAccount find(SessionUser actor, int userId) {
        AccessPolicy.requireAdmin(actor);
        return publicCopy(requireStaffAccount(userId));
    }

    public UserAccount register(SessionUser actor, String username, String password, String fullName, String email, String contactNumber) {
        AccessPolicy.requireAdmin(actor);
        UserAccount staff = new UserAccount();
        staff.setUsername(FieldValidator.username(username));
        staff.setPasswordHash(PasswordHasher.hash(FieldValidator.password(password)));
        staff.setFullName(FieldValidator.personName(fullName, "Full name"));
        staff.setEmail(FieldValidator.email(email));
        staff.setContactNumber(FieldValidator.contactNumber(contactNumber));
        staff.setRole(Role.STAFF);
        staff.setStatus(AccountStatus.ACTIVE);
        if (users.existsByUsername(staff.getUsername())) {
            throw new ConflictException("That username is already in use.");
        }
        if (users.existsByEmail(staff.getEmail(), null)) {
            throw new ConflictException("That email address is already in use.");
        }
        if (users.existsByContact(staff.getContactNumber(), null)) {
            throw new ConflictException("That contact number is already in use.");
        }
        staff.setUserId(users.insert(staff));
        return publicCopy(staff);
    }

    public UserAccount update(SessionUser actor, int userId, String fullName, String email, String contactNumber, String password) {
        AccessPolicy.requireAdmin(actor);
        UserAccount staff = requireStaffAccount(userId);
        staff.setFullName(FieldValidator.personName(fullName, "Full name"));
        staff.setEmail(FieldValidator.email(email));
        staff.setContactNumber(FieldValidator.contactNumber(contactNumber));
        if (users.existsByEmail(staff.getEmail(), staff.getUserId())) {
            throw new ConflictException("That email address is already in use.");
        }
        if (users.existsByContact(staff.getContactNumber(), staff.getUserId())) {
            throw new ConflictException("That contact number is already in use.");
        }
        String replacement = FieldValidator.optionalPassword(password);
        if (replacement != null) {
            staff.setPasswordHash(PasswordHasher.hash(replacement));
            tokens.deleteByUserId(staff.getUserId());
        }
        users.update(staff);
        return publicCopy(staff);
    }

    public UserAccount changeStatus(SessionUser actor, int userId, AccountStatus status) {
        AccessPolicy.requireAdmin(actor);
        if (status == null) {
            throw new ValidationException("Account status is required.");
        }
        UserAccount staff = requireStaffAccount(userId);
        if (actor.getUserId() == staff.getUserId()) {
            throw new ForbiddenException("You cannot change the status of your own account.");
        }
        users.updateStatus(staff.getUserId(), status);
        staff.setStatus(status);
        if (status == AccountStatus.BLOCKED) {
            tokens.deleteByUserId(staff.getUserId());
        }
        return publicCopy(staff);
    }

    private UserAccount requireStaffAccount(int userId) {
        UserAccount account = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("Staff member was not found."));
        if (account.getRole() != Role.STAFF) {
            throw new ForbiddenException("Administrator accounts cannot be managed from Staff Management.");
        }
        return account;
    }

    private UserAccount publicCopy(UserAccount source) {
        UserAccount copy = new UserAccount();
        copy.setUserId(source.getUserId());
        copy.setUsername(source.getUsername());
        copy.setFullName(source.getFullName());
        copy.setEmail(source.getEmail());
        copy.setContactNumber(source.getContactNumber());
        copy.setRole(source.getRole());
        copy.setStatus(source.getStatus());
        copy.setCreatedAt(source.getCreatedAt());
        return copy;
    }
}
