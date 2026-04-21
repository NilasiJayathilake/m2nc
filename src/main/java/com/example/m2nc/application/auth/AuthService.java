package com.example.m2nc.application.auth;

import com.example.m2nc.domain.user.RefreshToken;
import com.example.m2nc.domain.user.RefreshTokenRepository;
import com.example.m2nc.domain.user.RefreshTokenStatus;
import com.example.m2nc.domain.user.User;
import com.example.m2nc.domain.user.UserRepository;
import com.example.m2nc.domain.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Duration NEW_COMER_ACCESS_TOKEN_TTL = Duration.ofMinutes(30);
    private static final Duration FRIEND_ACCESS_TOKEN_TTL = Duration.ofHours(12);
    private static final Duration NEW_COMER_REFRESH_TOKEN_TTL = Duration.ofDays(7);
    private static final Duration FRIEND_REFRESH_TOKEN_TTL = Duration.ofDays(30);
    private static final int TOKEN_LENGTH_BYTES = 32;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthResponse signUp(SignUpRequest request) {
        validateSignUpRequest(request);

        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalStateException("Email is already registered");
        }

        UserRole role = request.role() == null ? UserRole.NEW_COMER : request.role();
        User user = userRepository.save(
                User.builder()
                        .name(request.name().trim())
                        .email(normalizedEmail)
                        .passwordHash(passwordEncoder.encode(request.password()))
                        .role(role)
                        .build());

        return issueTokens(user);
    }

    public AuthResponse signIn(SignInRequest request) {
        validateSignInRequest(request);

        String normalizedEmail = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return issueTokens(user);
    }

    public void logOut(LogOutRequest request) {
        if (request == null || !StringUtils.hasText(request.refreshToken())) {
            throw new IllegalArgumentException("Refresh token is required");
        }

        RefreshToken token = refreshTokenRepository
                .findByRefreshTokenAndStatus(request.refreshToken(), RefreshTokenStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token is invalid or already revoked"));

        LocalDateTime now = LocalDateTime.now();
        if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(now)) {
            token.setStatus(RefreshTokenStatus.EXPIRED);
            token.setLastUsedAt(now);
            refreshTokenRepository.save(token);
            return;
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found for refresh token"));

        if (request.logOutAllSessions() && user.getRole() == UserRole.FRIEND) {
            revokeAllActiveTokens(user.getId(), now);
            return;
        }

        token.setStatus(RefreshTokenStatus.REVOKED);
        token.setLastUsedAt(now);
        refreshTokenRepository.save(token);
    }

    private AuthResponse issueTokens(User user) {
        LocalDateTime now = LocalDateTime.now();
        revokeAllActiveTokens(user.getId(), now);

        LocalDateTime accessTokenExpiresAt = now.plus(accessTokenTtl(user.getRole()));
        LocalDateTime refreshTokenExpiresAt = now.plus(refreshTokenTtl(user.getRole()));

        String accessToken = generateSecureToken();
        String refreshTokenValue = generateSecureToken();

        RefreshToken refreshToken = refreshTokenRepository.save(
                RefreshToken.builder()
                        .userId(user.getId())
                        .refreshToken(refreshTokenValue)
                        .status(RefreshTokenStatus.ACTIVE)
                        .refreshCount(0)
                        .expiresAt(refreshTokenExpiresAt)
                        .lastUsedAt(now)
                        .build());

        return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                accessToken,
                accessTokenExpiresAt,
                refreshToken.getRefreshToken(),
                refreshToken.getExpiresAt());
    }

    private void revokeAllActiveTokens(String userId, LocalDateTime now) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserIdAndStatus(userId,
                RefreshTokenStatus.ACTIVE);
        if (activeTokens.isEmpty()) {
            return;
        }

        activeTokens.forEach(token -> {
            token.setStatus(RefreshTokenStatus.REVOKED);
            token.setLastUsedAt(now);
        });

        refreshTokenRepository.saveAll(activeTokens);
    }

    private Duration accessTokenTtl(UserRole role) {
        if (role == UserRole.FRIEND) {
            return FRIEND_ACCESS_TOKEN_TTL;
        }
        return NEW_COMER_ACCESS_TOKEN_TTL;
    }

    private Duration refreshTokenTtl(UserRole role) {
        if (role == UserRole.FRIEND) {
            return FRIEND_REFRESH_TOKEN_TTL;
        }
        return NEW_COMER_REFRESH_TOKEN_TTL;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[TOKEN_LENGTH_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void validateSignUpRequest(SignUpRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }
        if (!StringUtils.hasText(request.name())) {
            throw new IllegalArgumentException("Name is required");
        }
        if (!StringUtils.hasText(request.email())) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!StringUtils.hasText(request.password()) || request.password().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
    }

    private void validateSignInRequest(SignInRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }
        if (!StringUtils.hasText(request.email()) || !StringUtils.hasText(request.password())) {
            throw new IllegalArgumentException("Email and password are required");
        }
    }

    public record SignUpRequest(
            String name,
            String email,
            String password,
            UserRole role) {
    }

    public record SignInRequest(
            String email,
            String password) {
    }

    public record LogOutRequest(
            String refreshToken,
            boolean logOutAllSessions) {
    }

    public record AuthResponse(
            String userId,
            String name,
            String email,
            UserRole role,
            String accessToken,
            LocalDateTime accessTokenExpiresAt,
            String refreshToken,
            LocalDateTime refreshTokenExpiresAt) {
    }
}