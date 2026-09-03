package com.sunrise.clinic.support;

import com.sunrise.clinic.dao.UserDao;
import com.sunrise.clinic.model.AccountStatus;
import com.sunrise.clinic.model.Role;
import com.sunrise.clinic.model.UserAccount;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryUserDao implements UserDao {
    private final List<UserAccount> store = new ArrayList<>();
    private final AtomicInteger sequence = new AtomicInteger(1);

    @Override
    public Optional<UserAccount> findById(int userId) {
        return store.stream().filter(user -> user.getUserId() == userId).findFirst();
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return store.stream().filter(user -> user.getUsername().equalsIgnoreCase(username)).findFirst();
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return store.stream().filter(user -> user.getEmail().equalsIgnoreCase(email)).findFirst();
    }

    @Override
    public Optional<UserAccount> findByContact(String contactNumber) {
        return store.stream().filter(user -> user.getContactNumber().equals(contactNumber)).findFirst();
    }

    @Override
    public List<UserAccount> findStaffMembers() {
        return store.stream().filter(user -> user.getRole() == Role.STAFF).toList();
    }

    @Override
    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    @Override
    public boolean existsByEmail(String email, Integer excludeUserId) {
        return store.stream().anyMatch(user -> user.getEmail().equalsIgnoreCase(email)
                && (excludeUserId == null || user.getUserId() != excludeUserId));
    }

    @Override
    public boolean existsByContact(String contactNumber, Integer excludeUserId) {
        return store.stream().anyMatch(user -> user.getContactNumber().equals(contactNumber)
                && (excludeUserId == null || user.getUserId() != excludeUserId));
    }

    @Override
    public int insert(UserAccount user) {
        user.setUserId(sequence.getAndIncrement());
        user.setCreatedAt(LocalDateTime.now());
        store.add(user);
        return user.getUserId();
    }

    @Override
    public void update(UserAccount user) {
        findById(user.getUserId()).ifPresent(existing -> {
            existing.setFullName(user.getFullName());
            existing.setEmail(user.getEmail());
            existing.setContactNumber(user.getContactNumber());
            existing.setPasswordHash(user.getPasswordHash());
        });
    }

    @Override
    public void updateStatus(int userId, AccountStatus status) {
        findById(userId).ifPresent(user -> user.setStatus(status));
    }

    @Override
    public boolean hasRole(Role role) {
        return store.stream().anyMatch(user -> user.getRole() == role);
    }
}
