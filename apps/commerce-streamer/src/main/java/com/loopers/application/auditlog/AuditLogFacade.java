package com.loopers.application.auditlog;

import com.loopers.domain.auditlog.AuditLogService;
import com.loopers.domain.eventhandled.EventHandledDomainType;
import com.loopers.domain.eventhandled.EventHandledService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AuditLogFacade {

    private final AuditLogService auditLogService;
    private final EventHandledService eventHandledService;

    private static final EventHandledDomainType DOMAIN_TYPE = EventHandledDomainType.AUDIT_LOG;

    @Transactional
    public void processAuditLog(AuditLogCommand command) {
        if (eventHandledService.isEventHandled(command.eventId(), DOMAIN_TYPE)) {
            return;
        }

        auditLogService.saveAuditLog(
                command.eventId(),
                command.userId(),
                command.actionType(),
                command.targetType(),
                command.targetId(),
                command.payload()
        );

        eventHandledService.saveEventHandled(command.eventId(), DOMAIN_TYPE, command.eventType());
    }
}
