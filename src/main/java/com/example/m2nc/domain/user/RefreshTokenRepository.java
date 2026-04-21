package com.example.m2nc.domain.user;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
    Optional<RefreshToken> findByRefreshTokenAndStatus(String refreshToken, RefreshTokenStatus status);

    List<RefreshToken> findAllByUserIdAndStatus(String userId, RefreshTokenStatus status);
}
