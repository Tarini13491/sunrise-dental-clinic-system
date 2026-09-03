package com.sunrise.clinic.dao;

import com.sunrise.clinic.model.AuthToken;

import java.util.Optional;

public interface AuthTokenDao {
    Optional<AuthToken> findByHash(String tokenHash);

    int insert(AuthToken token);

    void deleteByHash(String tokenHash);

    void deleteByUserId(int userId);
}
