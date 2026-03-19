package com.domuspacis.auth.infrastructure;

import com.domuspacis.auth.domain.SecurityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SecurityAuditLogRepository extends JpaRepository<SecurityAuditLog, UUID> {
    long countByUsernameAndEventTypeAndTimestampAfter(
            String username, String eventType, java.time.Instant since);
}
