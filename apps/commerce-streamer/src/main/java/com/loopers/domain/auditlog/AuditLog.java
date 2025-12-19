package com.loopers.domain.auditlog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "audit_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", unique = true)
    private String eventId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "action_type")
    private String actionType;

    @Column(name = "target_type")
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public static AuditLog create(
            String eventId,
            Long userId,
            String actionType,
            String targetType,
            Long targetId,
            String payload
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.eventId = eventId;
        auditLog.userId = userId;
        auditLog.actionType = actionType;
        auditLog.targetType = targetType;
        auditLog.targetId = targetId;
        auditLog.payload = payload;
        auditLog.createdAt = LocalDateTime.now();
        return auditLog;
    }
}
