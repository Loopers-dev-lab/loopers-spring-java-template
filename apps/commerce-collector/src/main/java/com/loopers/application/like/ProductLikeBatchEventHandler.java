package com.loopers.application.like;

import com.loopers.application.eventhandled.EventHandledFacade;
import com.loopers.application.eventhandled.EventHandledInfo;
import com.loopers.application.metrics.ProductMetricsDailyFacade;
import com.loopers.application.metrics.ProductMetricsFacade;
import com.loopers.interfaces.consumer.like.dto.ProductLikeEvent;
import com.loopers.kafka.AggregateTypes;
import com.loopers.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductLikeBatchEventHandler {

    private final EventHandledFacade eventHandledFacade;
    private final ProductMetricsFacade productMetricsFacade;
    private final ProductMetricsDailyFacade productMetricsDailyFacade;

    @Transactional
    public void handleProductLikeBatch(List<ProductLikeEvent> events) {
        // 자정 경계 문제 방지: 트랜잭션 시작 시점의 날짜를 캡처하여 일관성 보장
        LocalDate processingDate = LocalDate.now();

        try {
            log.info("좋아요 배치 처리 시작 - 전체 이벤트 수: {}, 처리 날짜: {}",
                events.size(), processingDate);

            List<ProductLikeEvent> unprocessedEvents = filterUnprocessedEvents(events);
            if (unprocessedEvents.isEmpty()) {
                log.info("처리할 이벤트 없음 (모두 중복)");
                return;
            }

            Map<String, ProductLikeEvent> uniqueEvents = removeDuplicates(unprocessedEvents);
            Map<Long, Integer> likeDeltas = calculateLikeDeltas(uniqueEvents.values());

            log.info("증감량 계산 완료 - 처리 대상 상품 수: {}", likeDeltas.size());

            updateMetrics(likeDeltas, processingDate);
            markEventsAsHandled(uniqueEvents.values());

            log.info("좋아요 배치 처리 완료 - 전체: {}, 미처리: {}, 실제 처리: {}",
                    events.size(), unprocessedEvents.size(), uniqueEvents.size());

        } catch (Exception e) {
            log.error("좋아요 배치 처리 실패 - 전체 트랜잭션 롤백됨 | 이벤트 수: {}", events.size(), e);
            throw new RuntimeException("좋아요 배치 처리 실패", e);
        }
    }

    private List<ProductLikeEvent> filterUnprocessedEvents(List<ProductLikeEvent> events) {
        // N+1 방지: 한 번의 쿼리로 모든 처리된 이벤트 ID 조회
        List<String> eventIds = events.stream()
                .map(ProductLikeEvent::eventId)
                .collect(Collectors.toList());

        Set<String> handledEventIds = eventHandledFacade.findAlreadyHandledEventIds(eventIds);

        return events.stream()
                .filter(event -> !handledEventIds.contains(event.eventId()))
                .collect(Collectors.toList());
    }

    private Map<String, ProductLikeEvent> removeDuplicates(List<ProductLikeEvent> events) {
        return events.stream()
                .collect(Collectors.toMap(
                        ProductLikeEvent::eventId,
                        event -> event,
                        (existing, replacement) -> existing
                ));
    }

    private Map<Long, Integer> calculateLikeDeltas(Iterable<ProductLikeEvent> events) {
        Map<Long, Integer> likeDeltas = new HashMap<>();

        for (ProductLikeEvent event : events) {
            int delta = calculateDelta(event.eventType());
            likeDeltas.merge(event.productId(), delta, Integer::sum);
        }

        return likeDeltas;
    }

    private int calculateDelta(String eventType) {
        if (KafkaTopics.ProductLike.LIKE_ADDED.equals(eventType)) {
            return 1;
        } else if (KafkaTopics.ProductLike.LIKE_REMOVED.equals(eventType)) {
            return -1;
        }
        return 0;
    }

    private void updateMetrics(Map<Long, Integer> likeDeltas, LocalDate processingDate) {
        productMetricsFacade.updateLikeCountBatch(likeDeltas);
        log.info("ProductMetrics 업데이트 완료");

        productMetricsDailyFacade.updateLikeDeltaBatch(likeDeltas, processingDate);
        log.info("ProductMetricsDaily 업데이트 완료 - 처리 날짜: {}", processingDate);
    }

    private void markEventsAsHandled(Iterable<ProductLikeEvent> events) {
        List<EventHandledInfo> eventHandledInfos = createEventHandledInfos(events);
        eventHandledFacade.markAsHandledBatch(eventHandledInfos);
        log.info("이벤트 처리 완료 기록");
    }

    private List<EventHandledInfo> createEventHandledInfos(Iterable<ProductLikeEvent> events) {
        List<EventHandledInfo> infos = new ArrayList<>();
        for (ProductLikeEvent event : events) {
            infos.add(EventHandledInfo.of(
                    event.eventId(),
                    event.eventType(),
                    AggregateTypes.PRODUCT_LIKE,
                    event.productId().toString()
            ));
        }
        return infos;
    }
}
