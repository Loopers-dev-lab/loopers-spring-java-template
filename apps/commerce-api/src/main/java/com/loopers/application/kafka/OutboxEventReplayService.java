package com.loopers.application.kafka;

import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import com.loopers.domain.outbox.OutboxEventStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventReplayService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaEventPublisher kafkaEventPublisher;

    @Scheduled(fixedDelay = 5000) // 5초마다 실행
    public void replayPendingEvents() {
        try {
            List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEventsOrderedByAggregate();
            
            if (pendingEvents.isEmpty()) {
                return;
            }

            log.info("Found {} pending events to replay", pendingEvents.size());

            // aggregate별로 그룹핑하여 순서 보장
            Map<String, List<OutboxEvent>> eventsByAggregate = pendingEvents.stream()
                .collect(Collectors.groupingBy(
                    event -> event.getAggregateType() + ":" + event.getAggregateId(),
                    Collectors.toList()
                ));

            // 각 aggregate별로 순차 처리
            eventsByAggregate.forEach((aggregateKey, events) -> {
                log.info("Processing {} events for aggregate: {}", events.size(), aggregateKey);
                events.forEach(kafkaEventPublisher::publishEvent);
            });

        } catch (Exception e) {
            log.error("Error during event replay", e);
        }
    }

    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    public void retryFailedEvents() {
        try {
            ZonedDateTime oneHourAgo = ZonedDateTime.now().minusHours(1);
            List<OutboxEvent> failedEvents = outboxEventRepository.findFailedEventsForRetry(oneHourAgo);
            
            if (failedEvents.isEmpty()) {
                return;
            }

            log.info("Found {} failed events to retry", failedEvents.size());

            failedEvents.forEach(event -> {
                if (event.canRetry()) {
                    event.retry();
                    outboxEventRepository.save(event);
                    log.info("Retrying failed event: {}", event.getId());
                }
            });

        } catch (Exception e) {
            log.error("Error during failed event retry", e);
        }
    }

    @Scheduled(cron = "0 0 2 * * ?") // 매일 새벽 2시 실행
    public void cleanupOldProcessedEvents() {
        try {
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            List<OutboxEvent> oldProcessedEvents = outboxEventRepository.findProcessedEventsBefore(sevenDaysAgo);
            
            if (!oldProcessedEvents.isEmpty()) {
                outboxEventRepository.deleteAll(oldProcessedEvents);
                log.info("Cleaned up {} old processed events", oldProcessedEvents.size());
            }

        } catch (Exception e) {
            log.error("Error during cleanup of old processed events", e);
        }
    }
}