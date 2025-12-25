package com.loopers.infrastructure.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    public static final String HEADER_OUTBOX_ID = "outbox-id";
    public static final String HEADER_EVENT_TYPE = "event-type";
    private static final int BATCH_SIZE = 100;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 1000)
    public void relay() {
        List<Outbox> pendingList = outboxRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING,
                PageRequest.of(0, BATCH_SIZE)
        );

        for (Outbox outbox : pendingList) {
            try {
                String partitionKey = buildPartitionKey(outbox);
                ProducerRecord<Object, Object> record = new ProducerRecord<>(
                        outbox.getTopic(),
                        partitionKey,
                        outbox.getPayload()
                );
                record.headers().add(HEADER_OUTBOX_ID, outbox.getId().toString().getBytes(StandardCharsets.UTF_8));
                record.headers().add(HEADER_EVENT_TYPE, outbox.getEventType().getBytes(StandardCharsets.UTF_8));

                kafkaTemplate.send(record).get();
                outbox.markProcessed();
                outboxRepository.save(outbox);
                log.info("Outbox 발행 성공: id={}, topic={}, key={}", outbox.getId(), outbox.getTopic(), partitionKey);
            } catch (Exception e) {
                log.error("Outbox 발행 실패: id={}, topic={}", outbox.getId(), outbox.getTopic(), e);
            }
        }
    }

    private String buildPartitionKey(Outbox outbox) {
        try {
            JsonNode node = objectMapper.readTree(outbox.getPayload());
            JsonNode userIdNode = node.get("userId");
            if (userIdNode != null) {
                return outbox.getAggregateId() + ":" + userIdNode.asText();
            }
        } catch (Exception e) {
            log.warn("파티션 키 생성 실패, 기본 키 사용: {}", outbox.getAggregateId(), e);
        }
        return outbox.getAggregateId();
    }
}
