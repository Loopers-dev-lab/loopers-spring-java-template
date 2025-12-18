package com.loopers.kafka.learning;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.test.context.EmbeddedKafka;

/**
 * 학습 테스트 1: Auto Commit vs Manual Commit
 *
 * 학습 목표:
 * - Auto Commit의 동작 방식과 메시지 유실 가능성 이해
 * - Manual Commit의 정확한 제어 방식 이해
 * - 커밋 타이밍이 메시지 처리에 미치는 영향 체험
 *
 * 참고: @EmbeddedKafka를 사용하여 테스트 실행 시 자동으로 Kafka가 시작됩니다.
 */
@DisplayName("학습 1: Auto Commit vs Manual Commit")
@EmbeddedKafka(
    partitions = 1,
    topics = {"learning-auto-commit"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
public class Experiment1_AutoCommitTest {

    private static final Logger log = LoggerFactory.getLogger(Experiment1_AutoCommitTest.class);
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String TOPIC = "learning-auto-commit";

    @Test
    @DisplayName("Auto Commit: 처리 중 실패 시 메시지 유실")
    void autoCommit_MessageLoss() throws Exception {
        log.info("\n");
        log.info("========================================");
        log.info("실험 시작: Auto Commit 메시지 유실 시나리오");
        log.info("========================================");

        // 1. 메시지 전송
        log.info("\n=== 1단계: Producer가 메시지 10개 전송 ===");
        produceMessages(10);
        Thread.sleep(1000);

        // 2. Auto Commit Consumer
        log.info("\n=== 2단계: Auto Commit Consumer 시작 ===");
        log.info("설정: enable.auto.commit=true, auto.commit.interval=3초");

        Properties props = createConsumerProps("auto-commit-group");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "3000");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try {
            try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
                consumer.subscribe(Collections.singletonList(TOPIC));

                log.info("\n=== poll() 호출 ===");
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
                log.info("읽은 메시지 개수: {}", records.count());

                int count = 0;
                for (ConsumerRecord<String, String> record : records) {
                    count++;
                    log.info("처리 중: offset={}, value={}", record.offset(), record.value());

                    if (count == 5) {
                        log.error("\n!!! 5번째 메시지에서 에러 발생 !!!");
                        log.error("처리 완료: 0~4 (5개)");
                        log.error("처리 실패: 5~9 (5개)");
                        Thread.sleep(4000);  // 4초 대기 (auto commit interval 초과)
                        throw new RuntimeException("Processing failed at message 5");
                    }
                }
            }
        } catch (Exception e) {
            log.error("\n=== Consumer 종료 (에러로 인한 종료) ===");
        }

        // 3. 재시작
        log.info("\n=== 3단계: Consumer 재시작 ===");
        Thread.sleep(2000);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(TOPIC));

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            log.info("재시작 후 읽은 메시지 개수: {}", records.count());

            if (records.isEmpty()) {
                log.error("\n========================================");
                log.error("결과: 메시지 유실 발생!");
                log.error("========================================");
                log.error("- 5~9번 메시지는 처리 안 됐지만 커밋됨");
                log.error("- 재시작 시 이미 커밋된 오프셋부터 읽음");
                log.error("- 결과: 5~9번 메시지 영구 유실");
                log.error("\n학습: Auto Commit은 처리 성공 여부와 무관하게");
                log.error("      시간 기반으로 자동 커밋됨!");
            } else {
                records.forEach(record -> {
                    log.info("재시작 후 읽음: offset={}, value={}", record.offset(), record.value());
                });
            }
        }
    }

    @Test
    @DisplayName("Manual Commit: 처리 성공 시만 커밋")
    void manualCommit_NoMessageLoss() throws Exception {
        log.info("\n");
        log.info("========================================");
        log.info("실험 시작: Manual Commit 안전한 처리");
        log.info("========================================");

        // 1. 메시지 전송
        log.info("\n=== 1단계: Producer가 메시지 10개 전송 ===");
        produceMessages(10);
        Thread.sleep(1000);

        // 2. Manual Commit Consumer
        log.info("\n=== 2단계: Manual Commit Consumer 시작 ===");
        log.info("설정: enable.auto.commit=false");

        Properties props = createConsumerProps("manual-commit-group");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try {
            try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
                consumer.subscribe(Collections.singletonList(TOPIC));

                log.info("\n=== poll() 호출 ===");
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
                log.info("읽은 메시지 개수: {}", records.count());

                int count = 0;
                for (ConsumerRecord<String, String> record : records) {
                    count++;
                    log.info("처리 중: offset={}, value={}", record.offset(), record.value());

                    if (count == 5) {
                        log.error("\n!!! 5번째 메시지에서 에러 발생 !!!");
                        log.error("처리 완료: 0~4 (5개)");
                        log.error("처리 실패: 5~9 (5개)");
                        log.error("커밋하지 않고 종료");
                        throw new RuntimeException("Processing failed at message 5");
                    }
                }

                consumer.commitSync();
                log.info("커밋 완료");
            }
        } catch (Exception e) {
            log.error("\n=== Consumer 종료 (커밋 안 됨!) ===");
        }

        // 3. 재시작
        log.info("\n=== 3단계: Consumer 재시작 ===");
        Thread.sleep(2000);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(TOPIC));

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            log.info("재시작 후 읽은 메시지 개수: {}", records.count());

            log.info("\n========================================");
            log.info("결과: 메시지 유실 없음!");
            log.info("========================================");
            log.info("- 커밋하지 않았으므로 처음부터 다시 읽음");
            log.info("- 0~9번 메시지 모두 재처리 가능");
            log.info("- 결과: At Least Once 보장");
            log.info("\n학습: Manual Commit은 명시적으로 호출해야 커밋");
            log.info("      처리 실패 시 커밋 안 하면 재처리 가능!");
            log.info("      단, 중복 처리 가능 (멱등성 필요)");

            records.forEach(record -> {
                log.info("재시작 후 읽음: offset={}, value={}", record.offset(), record.value());
            });
        }
    }

    // Helper methods
    private void produceMessages(int count) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            for (int i = 0; i < count; i++) {
                ProducerRecord<String, String> record = new ProducerRecord<>(
                    TOPIC,
                    "key-" + i,
                    "Message " + i
                );
                producer.send(record);
            }
            producer.flush();
            log.info("메시지 {}개 전송 완료", count);
        }
    }

    private Properties createConsumerProps(String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return props;
    }
}
