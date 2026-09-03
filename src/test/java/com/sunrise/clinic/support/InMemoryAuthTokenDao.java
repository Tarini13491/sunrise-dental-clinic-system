package com.sunrise.clinic.support;

import com.sunrise.clinic.dao.AuthTokenDao;
import com.sunrise.clinic.model.AuthToken;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryAuthTokenDao implements AuthTokenDao {
    private final List<AuthToken> store = new ArrayList<>();
    private final AtomicInteger sequence = new AtomicInteger(1);

    @Override
    public Optional<AuthToken> findByHash(String tokenHash) {
        return store.stream().filter(token -> token.getTokenHash().equals(tokenHash)).findFirst();
    }

    @Override
    public int insert(AuthToken token) {
        token.setTokenId(sequence.getAndIncrement());
        token.setCreatedAt(LocalDateTime.now());
        store.add(token);
        return token.getTokenId();
    }

    @Override
    public void deleteByHash(String tokenHash) {
        store.removeIf(token -> token.getTokenHash().equals(tokenHash));
    }

    @Override
    public void deleteByUserId(int userId) {
        store.removeIf(token -> token.getUserId() == userId);
    }
}
