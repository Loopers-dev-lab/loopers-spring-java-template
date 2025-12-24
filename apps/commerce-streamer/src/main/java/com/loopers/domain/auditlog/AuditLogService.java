package com.loopers.domain.auditlog;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void saveAuditLog(
            String eventId,
            Long userId,
            String actionType,
            String targetType,
            Long targetId,
            String payload
    ) {
        AuditLog auditLog = AuditLog.create(eventId, userId, actionType, targetType, targetId, payload);
        auditLogRepository.save(auditLog);
    }
}
