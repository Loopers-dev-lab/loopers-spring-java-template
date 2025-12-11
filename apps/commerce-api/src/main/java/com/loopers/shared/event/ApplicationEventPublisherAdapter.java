package com.loopers.shared.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * ApplicationEventPublisher 기반 구현체
 */
@Component
@RequiredArgsConstructor
public class ApplicationEventPublisherAdapter implements EventPublisher {
    
    private final ApplicationEventPublisher applicationEventPublisher;
    
    @Override
    public <T> void publish(String topic, String key, T event) {
        // 현재는 topic과 key를 무시하고 이벤트만 발행
        // topic과 key를 활용
        applicationEventPublisher.publishEvent(event);
    }
}

