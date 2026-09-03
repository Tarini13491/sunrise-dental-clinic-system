package com.sunrise.clinic.dao;

import com.sunrise.clinic.model.AccountStatus;
import com.sunrise.clinic.model.Role;
import com.sunrise.clinic.model.UserAccount;

import java.util.List;
import java.util.Optional;

public interface UserDao {
    Optional<UserAccount> findById(int userId);

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByContact(String contactNumber);

    List<UserAccount> findStaffMembers();

    boolean existsByUsername(String username);

    boolean existsByEmail(String email, Integer excludeUserId);

    boolean existsByContact(String contactNumber, Integer excludeUserId);

    int insert(UserAccount user);

    void update(UserAccount user);

    void updateStatus(int userId, AccountStatus status);

    boolean hasRole(Role role);
}
