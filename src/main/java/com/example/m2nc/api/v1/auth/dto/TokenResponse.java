package com.example.m2nc.api.v1.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    private String accessToken;
    private long expiresIn;
    @Builder.Default
    private String tokenType = "Bearer";
}
