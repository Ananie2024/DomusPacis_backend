package com.domuspacis.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "security_audit_log",
    indexes = {
        @Index(name = "idx_sec_audit_user", columnList = "username"),
        @Index(name = "idx_sec_audit_event", columnList = "event_type"),
        @Index(name = "idx_sec_audit_timestamp", columnList = "timestamp"),
        @Index(name = "idx_sec_audit_ip", columnList = "ip_address")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "event_type", length = 50, nullable = false)
    private String eventType;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "outcome", length = 20)
    private String outcome;

    @Column(name = "detail", length = 1000)
    private String detail;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
