package com.domuspacis.auth.domain;

import com.domuspacis.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(
    name = "token_blacklist",
    indexes = {
        @Index(name = "idx_tb_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_tb_user", columnList = "user_id"),
        @Index(name = "idx_tb_expires", columnList = "expires_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TokenBlacklist extends BaseEntity {

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "token_type", nullable = false, length = 16)
    private String tokenType;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "invalidated_at", nullable = false)
    private Instant invalidatedAt;

    @Column(name = "reason", nullable = false, length = 64)
    private String reason;
}