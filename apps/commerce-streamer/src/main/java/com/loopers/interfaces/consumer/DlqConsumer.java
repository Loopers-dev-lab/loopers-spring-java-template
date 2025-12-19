package com.loopers.interfaces.consumer;

import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.domain.dlq.DlqMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DlqConsumer {

    private final DlqMessageService dlqMessageService;

    @KafkaListener(
            topicPattern = ".*\\.DLT",
            groupId = "${kafka.consumer.dlq-group:dlq-consumer-group}",
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void consume(
            List<ConsumerRecord<String, String>> records,
            Acknowledgment acknowledgment
    ) {
        for (ConsumerRecord<String, String> record : records) {
            log.error("DLQ 메시지 수신 - topic: {}, partition: {}, offset: {}, key: {}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key()
            );

            processDlqRecord(record);
        }

        acknowledgment.acknowledge();
    }

    private void processDlqRecord(ConsumerRecord<String, String> record) {
        try {
            String originalTopic = extractOriginalTopic(record.topic());

            dlqMessageService.saveDlqMessage(
                    originalTopic,
                    record.partition(),
                    record.offset(),
                    record.key(),
                    record.value(),
                    "Message failed after max retries"
            );
        } catch (Exception e) {
            log.error("DLQ 메시지 저장 실패: topic={}, key={}", record.topic(), record.key(), e);
        }
    }

    private String extractOriginalTopic(String dlqTopic) {
        if (dlqTopic.endsWith(".DLT")) {
            return dlqTopic.substring(0, dlqTopic.length() - 4);
        }
        return dlqTopic;
    }
}
