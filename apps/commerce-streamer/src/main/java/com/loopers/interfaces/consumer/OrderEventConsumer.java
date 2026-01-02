package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.EventHandledService;
import com.loopers.application.metrics.MetricsAggregator;
import com.loopers.application.ranking.RankingAggregator;
import com.loopers.config.kafka.KafkaConfig;
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
public class OrderEventConsumer {

    private final ObjectMapper objectMapper;
    private final EventHandledService eventHandledService;
    private final MetricsAggregator metricsAggregator;
    private final RankingAggregator rankingAggregator;

    @KafkaListener(
            topics = "${kafka.topics.order-events}",
            groupId = "commerce-streamer-order",
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void consume(List<ConsumerRecord<String, String>> records) throws Exception {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Map<String, Object>> accepted = new ArrayList<>();
        for (ConsumerRecord<String, String> record : records) {

			Map<String, Object> event = readEvent(record.value());

            String eventId = (String) event.get("id");
            String eventType = (String) event.get("eventType");

            if (eventId == null) {
                log.warn("eventId 없음 → skip");
                continue;
            }

            // 멱등 처리
            if (!eventHandledService.tryMarkHandled(eventId, eventType)) {
                log.info("중복 이벤트 skip - {}", eventId);
                continue;
            }

            Map<String, Object> payload = (Map<String, Object>) event.get("payload");

            if (payload == null) {
                log.warn("payload 없음 - eventId={}", eventId);
                continue;
            }
            accepted.add(event);
        }

        if (!accepted.isEmpty()) {
            metricsAggregator.aggregate(accepted);
            rankingAggregator.aggregate(accepted);
        }

    }

	@SuppressWarnings("unchecked")
	private Map<String, Object> readEvent(String raw) throws Exception {
		if (raw == null) return null;
		String s = raw.trim();
		if (s.startsWith("\"") && s.endsWith("\"")) {
			s = objectMapper.readValue(s, String.class);
		}
		return objectMapper.readValue(s, Map.class);
	}
}


