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
 * 학습 테스트 3: max.poll.interval.ms
 *
 * 학습 목표:
 * - poll() 호출 간격이 타임아웃을 초과하면 리밸런싱 발생
 * - 처리 시간과 타임아웃의 관계 이해
 * - 배치 크기 조정으로 타임아웃 방지
 *
 * 주의: 일부 테스트는 시간이 오래 걸립니다 (약 2분)
 *
 * 참고: @EmbeddedKafka를 사용하여 테스트 실행 시 자동으로 Kafka가 시작됩니다.
 */
@DisplayName("학습 3: max.poll.interval.ms")
@EmbeddedKafka(
    partitions = 1,
    topics = {"learning-poll-interval"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
public class Experiment3_PollIntervalTest {

    private static final Logger log = LoggerFactory.getLogger(Experiment3_PollIntervalTest.class);
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String TOPIC = "learning-poll-interval";

    @Test
    @DisplayName("처리 시간이 타임아웃 초과 시 리밸런싱")
    void pollInterval_Timeout() throws Exception {
        log.info("\n");
        log.info("========================================");
        log.info("실험 시작: max.poll.interval.ms 타임아웃");
        log.info("========================================");
        log.info("⚠️  주의: 이 테스트는 약 2분 소요됩니다");

        // 1. 메시지 전송
        log.info("\n=== 1단계: 메시지 10개 전송 ===");
        produceMessages(10);
        Thread.sleep(1000);

        // 2. Consumer (짧은 타임아웃)
        log.info("\n=== 2단계: Consumer 시작 ===");
        log.info("설정: max.poll.interval.ms=30초");

        Properties props = createConsumerProps("timeout-group");
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "30000");  // 30초
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(TOPIC));

            log.info("\n=== 첫 번째 poll() ===");
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            log.info("읽은 메시지 개수: {}", records.count());

            records.forEach(record -> {
                log.info("읽음: offset={}, value={}", record.offset(), record.value());
            });

            log.info("\n=== 메시지 처리 시작 (40초 소요) ===");
            log.info("max.poll.interval.ms=30초 < 처리 시간 40초");
            log.info("타임아웃 초과 예정...");
            Thread.sleep(40000);  // 40초 대기

            log.info("\n=== 처리 완료, 다음 poll() 시도 ===");

            try {
                ConsumerRecords<String, String> nextRecords = consumer.poll(Duration.ofSeconds(5));
                log.info("두 번째 poll: 읽은 메시지 개수: {}", nextRecords.count());
            } catch (Exception e) {
                log.error("\n========================================");
                log.error("결과: 에러 발생!");
                log.error("========================================");
                log.error("에러 타입: {}", e.getClass().getSimpleName());
                log.error("에러 메시지: {}", e.getMessage());
                log.error("\n원인:");
                log.error("- 첫 poll() 후 40초 경과");
                log.error("- max.poll.interval.ms=30초 초과");
                log.error("- Kafka가 Consumer를 Group에서 제거");
                log.error("- 다른 Consumer에게 파티션 재할당");
                log.error("\n학습: 처리 시간 < max.poll.interval.ms 필수!");
                log.error("      타임아웃 늘리거나 배치 크기 줄이기");
            }
        }
    }

    @Test
    @DisplayName("타임아웃 내에 처리하면 정상 동작")
    void pollInterval_Normal() throws Exception {
        log.info("\n");
        log.info("========================================");
        log.info("실험 시작: 정상적인 poll 간격");
        log.info("========================================");

        // 1. 메시지 전송
        log.info("\n=== 1단계: 메시지 10개 전송 ===");
        produceMessages(10);
        Thread.sleep(1000);

        // 2. Consumer (충분한 타임아웃)
        log.info("\n=== 2단계: Consumer 시작 ===");
        log.info("설정: max.poll.interval.ms=5분");

        Properties props = createConsumerProps("normal-group");
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "300000");  // 5분
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(TOPIC));

            log.info("\n=== 첫 번째 poll() ===");
            ConsumerRecords<String, String> records1 = consumer.poll(Duration.ofSeconds(5));
            log.info("읽은 메시지 개수: {}", records1.count());

            log.info("\n=== 메시지 처리 (10초 소요) ===");
            log.info("max.poll.interval.ms=5분 > 처리 시간 10초");
            Thread.sleep(10000);
            log.info("처리 완료");

            consumer.commitSync();
            log.info("커밋 완료");

            log.info("\n=== 두 번째 poll() ===");
            ConsumerRecords<String, String> records2 = consumer.poll(Duration.ofSeconds(5));
            log.info("읽은 메시지 개수: {} (정상)", records2.count());

            log.info("\n========================================");
            log.info("결과: 정상 동작!");
            log.info("========================================");
            log.info("- 처리 시간 10초 < 타임아웃 5분");
            log.info("- 정상적으로 커밋 및 다음 poll 성공");
            log.info("\n학습: 타임아웃 내에 처리하면 문제없음");
            log.info("      여유 있게 설정하되, 너무 길면 장애 감지 지연");
        }
    }

    @Test
    @DisplayName("배치 크기를 줄여서 타임아웃 방지")
    void pollInterval_SmallBatch() throws Exception {
        log.info("\n");
        log.info("========================================");
        log.info("실험 시작: 배치 크기로 타임아웃 방지");
        log.info("========================================");

        // 1. 메시지 전송
        log.info("\n=== 1단계: 메시지 100개 전송 ===");
        produceMessages(100);
        Thread.sleep(2000);

        // 2. Consumer (작은 배치)
        log.info("\n=== 2단계: Consumer 시작 ===");
        log.info("설정: max.poll.interval.ms=30초, max.poll.records=10개");

        Properties props = createConsumerProps("small-batch-group");
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "30000");  // 30초
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "10");  // 10개씩
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(TOPIC));

            int totalProcessed = 0;
            for (int i = 0; i < 5; i++) {
                log.info("\n=== poll() #{} ===", i + 1);
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
                log.info("읽은 메시지 개수: {}", records.count());
                totalProcessed += records.count();

                // 메시지 1개당 2초 처리 가정
                // 10개 × 2초 = 20초 (타임아웃 30초 내)
                log.info("처리 중... (약 20초)");
                Thread.sleep(20000);

                consumer.commitSync();
                log.info("커밋 완료 (총 {}개 처리)", totalProcessed);
            }

            log.info("\n========================================");
            log.info("결과: 타임아웃 없이 성공!");
            log.info("========================================");
            log.info("- 배치 크기: 10개");
            log.info("- 배치당 처리 시간: 20초 < 타임아웃 30초");
            log.info("- 5번 poll로 50개 처리 완료");
            log.info("\n학습: 배치 크기로 타임아웃 제어 가능");
            log.info("      공식: 배치 크기 × 메시지 처리 시간 < max.poll.interval.ms");
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
