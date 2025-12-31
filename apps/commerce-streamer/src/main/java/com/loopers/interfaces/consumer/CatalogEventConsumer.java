package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.metrics.MetricsAggregator;
import com.loopers.application.ranking.RankingAggregator;
import com.loopers.application.EventHandledService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogEventConsumer {

    private final ObjectMapper objectMapper;
    private final EventHandledService eventHandledService;
    private final MetricsAggregator metricsAggregator;
    private final RankingAggregator rankingAggregator;

    @KafkaListener(
            topics = "${kafka.topics.catalog-events}",
            groupId = "commerce-streamer-catalog"
    )
    public void consume(List<ConsumerRecord<String, String>> records) throws Exception {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Map<String, Object>> accepted = new ArrayList<>();
        for (ConsumerRecord<String, String> record : records) {
            Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);

            String eventId = (String) event.get("id");
            String eventType = (String) event.get("eventType");
            Map<String, Object> payload = (Map<String, Object>) event.get("payload");
            if (eventId == null || eventType == null || payload == null) {
                log.warn("잘못된 카탈로그 이벤트 - eventType 또는 payload 없음");
                continue;
            }

            // 멱등 처리
            if (!eventHandledService.tryMarkHandled(eventId, eventType)) {
                log.info("중복 이벤트 skip - {}", eventId);
                continue;
            }
            accepted.add(event);
        }
        metricsAggregator.aggregate(accepted);
        rankingAggregator.aggregate(accepted);
    }
}


