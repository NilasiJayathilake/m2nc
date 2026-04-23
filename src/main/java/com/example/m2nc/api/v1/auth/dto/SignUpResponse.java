package com.example.m2nc.api.v1.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class SignUpResponse {
    private String message;
    private UserResponse userResponse;
}
