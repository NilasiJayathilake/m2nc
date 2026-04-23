package com.example.m2nc.application.auth;


import com.example.m2nc.api.v1.auth.dto.*;
import com.example.m2nc.domain.user.*;
import com.example.m2nc.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public SignUpResponse signUp(SignUpRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .isActive(true)
                .build();

        userRepository.save(user);

        return SignUpResponse.builder()
                .message("User registered successfully")
                .userResponse(UserResponse.fromUser(user))
                .build();
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            if (!user.isActive()) {
                throw new BadCredentialsException("User account is deactivated");
            }

            String accessToken = jwtTokenProvider.generateAccessToken(user);
            String refreshTokenValue = createRefreshToken(user);

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshTokenValue)
                    .expiresIn(jwtTokenProvider.getAccessTokenExpiration() / 1000)
                    .user(UserResponse.fromUser(user))
                    .build();
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw e;
        }
    }

    @Transactional
    public TokenResponse refreshToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByRefreshTokenAndStatus(refreshTokenValue, RefreshTokenStatus.EXPIRED)
                .orElseThrow(() -> new BadCredentialsException("Refresh token expired"));

        if (refreshToken.getStatus() == RefreshTokenStatus.REVOKED) {
            throw new BadCredentialsException("Refresh token has been revoked");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new BadCredentialsException("User account is deactivated");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration() / 1000)
                .build();
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByRefreshToken(refreshTokenValue)
                .ifPresent(token -> {
                    token.setStatus(RefreshTokenStatus.REVOKED);
                    refreshTokenRepository.save(token);
                });
    }

    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return UserResponse.fromUser(user);
    }

    private String createRefreshToken(User user) {

        refreshTokenRepository.deleteByUserId(user.getId());

        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(jwtTokenProvider.getRefreshTokenExpiration());

        RefreshToken refreshToken = RefreshToken.builder()
                .refreshToken(UUID.randomUUID().toString())
                .userId(user.getId())
                .expiresAt(expiresAt)
                .status(RefreshTokenStatus.ACTIVE)
                .build();

        refreshTokenRepository.save(refreshToken);
        return refreshToken.getRefreshToken();
    }
}
