package com.loopers.infrastructure.kafka.producer;

import com.loopers.infrastructure.kafka.dto.UserActionDto;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionEventProducer {

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Value("${kafka.topic.user-action-name}")
    private String userActionTopic;

    @Retry(name = "kafkaProducer", fallbackMethod = "userActionFallback")
    public void sendUserActionEvent(Long userId, String actionType, String targetType, Long targetId) {
        UserActionDto event = UserActionDto.of(userId, actionType, targetType, targetId);
        kafkaTemplate.send(userActionTopic, userId.toString(), event);
        log.info("유저 행동 이벤트 발행: userId={}, actionType={}, targetType={}, targetId={}",
                userId, actionType, targetType, targetId);
    }

    public void userActionFallback(Long userId, String actionType, String targetType, Long targetId, Throwable ex) {
        log.error("유저 행동 이벤트 발행 실패 (재시도 후): userId={}, actionType={}", userId, actionType, ex);
    }
}
