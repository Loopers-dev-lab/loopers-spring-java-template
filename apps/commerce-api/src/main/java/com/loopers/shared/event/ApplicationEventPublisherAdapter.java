package com.loopers.shared.event;

import com.loopers.shared.event.DomainEvent;
import com.loopers.shared.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Spring ApplicationEventPublisher 기반 구현체
 * 동일한 애플리케이션 내부에서 이벤트를 비동기적으로 처리할 때 사용합니다.
 */
@Component
@RequiredArgsConstructor
public class ApplicationEventPublisherAdapter implements EventPublisher {
    
    private final ApplicationEventPublisher applicationEventPublisher;
    
    @Override
    public void publish(DomainEvent event) {
        // Spring의 ApplicationEventPublisher를 통해 내부 이벤트 발행
        // topic과 key는 내부 이벤트에서는 사용하지 않음
        applicationEventPublisher.publishEvent(event);
    }
}

