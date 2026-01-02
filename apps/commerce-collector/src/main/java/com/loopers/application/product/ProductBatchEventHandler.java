package com.loopers.application.product;

import com.loopers.application.eventhandled.EventHandledFacade;
import com.loopers.application.eventhandled.EventHandledInfo;
import com.loopers.application.metrics.ProductMetricsDailyFacade;
import com.loopers.application.metrics.ProductMetricsFacade;
import com.loopers.interfaces.consumer.product.dto.ProductEvent;
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
public class ProductBatchEventHandler {

    private final EventHandledFacade eventHandledFacade;
    private final ProductMetricsFacade productMetricsFacade;
    private final ProductMetricsDailyFacade productMetricsDailyFacade;

    @Transactional
    public void handleProductViewBatch(List<ProductEvent> events) {
        // 자정 경계 문제 방지: 트랜잭션 시작 시점의 날짜를 캡처하여 일관성 보장
        LocalDate processingDate = LocalDate.now();

        try {
            log.info("상품 조회 이벤트 배치 처리 시작 - 전체 이벤트 수: {}, 처리 날짜: {}",
                events.size(), processingDate);

            List<ProductEvent> unprocessedEvents = filterUnprocessedEvents(events);

            if (unprocessedEvents.isEmpty()) {
                log.info("처리할 이벤트 없음 (모두 중복)");
                return;
            }

            Map<String, ProductEvent> uniqueEvents = removeDuplicates(unprocessedEvents);
            Map<Long, Integer> viewDeltas = calculateViewDeltas(uniqueEvents.values());

            log.info("증감량 계산 완료 - 처리 대상 상품 수: {}", viewDeltas.size());

            updateMetrics(viewDeltas, processingDate);
            markEventsAsHandled(uniqueEvents.values());

        } catch(Exception e) {
            log.error("상품 조회 이벤트 처리 실패 - 전체 트랜잭션 롤백됨 | 이벤트 수: {}", events.size(), e);
            throw new RuntimeException("상품 조회 이벤트 배치 처리 실패", e);
        }
    }

    private List<ProductEvent> filterUnprocessedEvents(List<ProductEvent> events) {
        // N+1 방지: 한 번의 쿼리로 모든 처리된 이벤트 ID 조회
        List<String> eventIds = events.stream()
                .map(ProductEvent::eventId)
                .collect(Collectors.toList());

        Set<String> handledEventIds = eventHandledFacade.findAlreadyHandledEventIds(eventIds);

        return events.stream()
                .filter(event -> !handledEventIds.contains(event.eventId()))
                .collect(Collectors.toList());
    }

    private Map<String, ProductEvent> removeDuplicates(List<ProductEvent> events) {
        return events.stream()
                .collect(Collectors.toMap(
                        ProductEvent::eventId,
                        event -> event,
                        (existing, replacement) -> existing
                ));
    }

    private Map<Long, Integer> calculateViewDeltas(Iterable<ProductEvent> events) {
        Map<Long, Integer> viewDeltas = new HashMap<>();

        for(ProductEvent event : events) {
            int delta = calculateDelta(event.eventType());
            viewDeltas.merge(event.productId(), delta, Integer::sum);
        }

        return viewDeltas;
    }

    private int calculateDelta(String eventType) {

        if(KafkaTopics.ProductDetail.PRODUCT_VIEWED.equals(eventType)) {
            return 1;
        }
        return 0;
    }

    private void updateMetrics(Map<Long, Integer> viewDeltas, LocalDate processingDate) {
        productMetricsFacade.updateViewCountBatch(viewDeltas);
        log.info("ProductMetrics 업데이트 완료");

        productMetricsDailyFacade.updateViewDeltaBatch(viewDeltas, processingDate);
        log.info("ProductMetricsDaily 업데이트 완료 - 처리 날짜: {}", processingDate);
    }

    private void markEventsAsHandled(Iterable<ProductEvent> events) {
        List<EventHandledInfo> eventHandledInfos = createEventHandledInfos(events);
        eventHandledFacade.markAsHandledBatch(eventHandledInfos);
        log.info("이벤트 처리 완료 기록");
    }

    private List<EventHandledInfo> createEventHandledInfos(Iterable<ProductEvent> events) {
        List<EventHandledInfo> infos = new ArrayList<>();
        for(ProductEvent event : events) {
            infos.add(EventHandledInfo.of(
                    event.eventId(),
                    event.eventType(),
                    AggregateTypes.PRODUCT_VIEW,
                    event.productId().toString()
            ));
        }

        return infos;
    }
}
