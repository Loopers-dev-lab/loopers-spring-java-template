package com.loopers.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class BusinessActionEventHandler {
    
    private final ObjectMapper objectMapper;

    @EventListener
    @Async
    public void handleBusinessActionEvent(BusinessActionEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            
            log.info("BUSINESS_ACTION_LOG={}", eventJson);
            
            if (event.action() == BusinessActionEvent.BusinessAction.PAYMENT_FAILED) {
                log.warn("PAYMENT_FAILURE_ALERT - 사용자: {}, 주문: {}, 금액: {}, 사유: {}", 
                    event.userId(), event.targetId(), event.amount(), event.metadata());
            }
            
        } catch (Exception e) {
            log.error("비즈니스 액션 이벤트 로깅 실패 - 사용자: {}, 액션: {}", 
                event.userId(), event.action(), e);
        }
    }
}