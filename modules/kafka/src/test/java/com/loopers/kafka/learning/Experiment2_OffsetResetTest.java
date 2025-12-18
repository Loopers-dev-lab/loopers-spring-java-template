package com.loopers.kafka.learning;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
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
 * 학습 테스트 2: auto.offset.reset
 *
 * 학습 목표:
 * - earliest: 파티션 처음부터 읽기
 * - latest: 최신 메시지만 읽기
 * - 기존 오프셋이 있을 때 auto.offset.reset이 무시됨을 이해
 *
 * 참고: @EmbeddedKafka를 사용하여 테스트 실행 시 자동으로 Kafka가 시작됩니다.
 */
@DisplayName("학습 2: auto.offset.reset")
@EmbeddedKafka(
    partitions = 1,
    topics = {"learning-offset-reset"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
public class Experiment2_OffsetResetTest {

    private static final Logger log = LoggerFactory.getLogger(Experiment2_OffsetResetTest.class);
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String TOPIC = "learning-offset-reset";

    @Test
    @DisplayName("earliest: 과거 메시지부터 읽기")
    void offsetReset_Earliest() throws Exception {
        log.info("\n");
        log.info("========================================");
        log.info("실험 시작: auto.offset.reset=earliest");
        log.info("========================================");

        // 1. Producer가 먼저 메시지 전송
        log.info("\n=== 1단계: Producer가 먼저 메시지 10개 전송 ===");
        produceMessages(10);
        Thread.sleep(2000);

        // 2. Consumer 시작 (earliest)
        log.info("\n=== 2단계: Consumer 시작 (earliest) ===");
        log.info("설정: auto.offset.reset=earliest");
        log.info("상황: 새 Consumer Group (오프셋 없음)");

        Properties props = createConsumerProps("earliest-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(TOPIC));

            log.info("\n=== poll() 호출 ===");
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
            log.info("읽은 메시지 개수: {}", records.count());

            records.forEach(record -> {
                log.info("읽음: offset={}, value={}", record.offset(), record.value());
            });

            log.info("\n========================================");
            log.info("결과: 과거 메시지 모두 읽음!");
            log.info("========================================");
            log.info("- Producer가 먼저 전송한 메시지 10개");
            log.info("- Consumer는 나중에 시작");
            log.info("- earliest 설정으로 파티션 처음부터 읽음");
            log.info("\n학습: earliest는 과거 데이터 처리에 유용");
            log.info("      - 테스트 환경");
            log.info("      - 데이터 복구");
            log.info("      - 새 Consumer 추가 시");
        }
    }

    @Test
    @DisplayName("latest: 새 메시지만 읽기")
    void offsetReset_Latest() throws Exception {
        log.info("\n");
        log.info("========================================");
        log.info("실험 시작: auto.offset.reset=latest");
        log.info("========================================");

        // 1. Producer가 먼저 메시지 전송
        log.info("\n=== 1단계: Producer가 먼저 메시지 10개 전송 ===");
        produceMessages(10);
        Thread.sleep(2000);

        // 2. Consumer 시작 (latest)
        log.info("\n=== 2단계: Consumer 시작 (latest) ===");
        log.info("설정: auto.offset.reset=latest");
        log.info("상황: 새 Consumer Group (오프셋 없음)");

        Properties props = createConsumerProps("latest-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(TOPIC));

            // 첫 poll: 과거 메시지는 안 읽음
            log.info("\n=== 첫 번째 poll() - 과거 메시지 ===");
            ConsumerRecords<String, String> oldRecords = consumer.poll(Duration.ofSeconds(5));
            log.info("읽은 메시지 개수: {} (과거 메시지 무시)", oldRecords.count());

            // 3. 새 메시지 전송
            log.info("\n=== 3단계: 새 메시지 5개 전송 ===");
            produceMessages(5);
            Thread.sleep(1000);

            // 두 번째 poll: 새 메시지는 읽음
            log.info("\n=== 두 번째 poll() - 새 메시지 ===");
            ConsumerRecords<String, String> newRecords = consumer.poll(Duration.ofSeconds(5));
            log.info("읽은 메시지 개수: {} (새 메시지만)", newRecords.count());

            newRecords.forEach(record -> {
                log.info("읽음: offset={}, value={}", record.offset(), record.value());
            });

            log.info("\n========================================");
            log.info("결과: 새 메시지만 읽음!");
            log.info("========================================");
            log.info("- 첫 poll: 0개 (과거 메시지 무시)");
            log.info("- 두 번째 poll: 5개 (새 메시지만)");
            log.info("\n학습: latest는 실시간 처리에 유용");
            log.info("      - 로그 모니터링");
            log.info("      - 알림 시스템");
            log.info("      - 과거 데이터 불필요한 경우");
        }
    }

    @Test
    @DisplayName("오프셋이 있으면 auto.offset.reset 무시됨")
    void offsetExists_IgnoresReset() throws Exception {
        log.info("\n");
        log.info("========================================");
        log.info("실험 시작: 기존 오프셋 존재 시");
        log.info("========================================");

        String groupId = "existing-offset-group";

        // 1. 첫 실행
        log.info("\n=== 1단계: 첫 실행 (오프셋 커밋) ===");
        produceMessages(10);
        Thread.sleep(1000);

        Properties props = createConsumerProps(groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(TOPIC));

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            log.info("첫 실행: 읽은 메시지 개수: {}", records.count());

            records.forEach(record -> {
                log.info("읽음: offset={}, value={}", record.offset(), record.value());
            });

            consumer.commitSync();
            log.info("오프셋 커밋 완료");
        }

        // 2. 재실행
        log.info("\n=== 2단계: 재실행 (같은 Consumer Group) ===");
        Thread.sleep(2000);

        produceMessages(5);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(TOPIC));

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            log.info("재실행: 읽은 메시지 개수: {}", records.count());

            records.forEach(record -> {
                log.info("읽음: offset={}, value={}", record.offset(), record.value());
            });

            log.info("\n========================================");
            log.info("결과: auto.offset.reset 무시됨!");
            log.info("========================================");
            log.info("- 설정: auto.offset.reset=earliest");
            log.info("- 하지만 기존 오프셋 존재");
            log.info("- 결과: 마지막 커밋 위치부터 읽음");
            log.info("\n학습: auto.offset.reset은 오프셋이 없을 때만 적용");
            log.info("      - 새 Consumer Group");
            log.info("      - 오프셋 만료");
            log.info("      - Consumer Group 삭제 후");
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
