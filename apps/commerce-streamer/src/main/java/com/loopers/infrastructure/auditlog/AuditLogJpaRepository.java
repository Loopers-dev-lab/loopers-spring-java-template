package com.loopers.infrastructure.auditlog;

import com.loopers.domain.auditlog.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogJpaRepository extends JpaRepository<AuditLog, Long> {
}
