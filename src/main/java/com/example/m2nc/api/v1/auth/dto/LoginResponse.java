package com.example.m2nc.api.v1.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    @Builder.Default
    private String tokenType = "Bearer";
    private UserResponse user;
}
