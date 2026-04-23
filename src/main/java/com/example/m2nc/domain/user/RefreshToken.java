package com.example.m2nc.domain.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "refresh_tokens")
public class RefreshToken {
    @Id
    private String id;
    private String userId;
    private String refreshToken;
    private RefreshTokenStatus status;
    private int refreshCount;

    @CreatedDate
    private LocalDateTime createdAt;
    private Instant expiresAt;
    private LocalDateTime lastUsedAt;

}
