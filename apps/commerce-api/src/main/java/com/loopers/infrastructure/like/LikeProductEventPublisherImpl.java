package com.loopers.infrastructure.like;

import com.loopers.domain.like.product.LikeProductEvent;
import com.loopers.domain.like.product.LikeProductEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LikeProductEventPublisherImpl implements LikeProductEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publishLikeEvent(LikeProductEvent event) {
        if (event == null) {
            log.warn("LikeProductEvent가 null입니다. 이벤트 발행을 건너뜁니다.");
            return;
        }
        log.info("LikeProductEvent 발행: {}", event);
        applicationEventPublisher.publishEvent(event);
    }
}
