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
 * 학습 테스트 4: max.poll.records
 *
 * 학습 목표:
 * - 배치 크기에 따른 처리량과 안정성 트레이드오프 이해
 * - 큰 배치 vs 작은 배치의 장단점 체험
 * - 최적 배치 크기 찾기
 *
 * 참고: @EmbeddedKafka를 사용하여 테스트 실행 시 자동으로 Kafka가 시작됩니다.
 */
@DisplayName("학습 4: max.poll.records")
@EmbeddedKafka(
    partitions = 1,
    topics = {"learning-batch-size"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
public class Experiment4_BatchSizeTest {

    private static final Logger log = LoggerFactory.getLogger(Experiment4_BatchSizeTest.class);
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String TOPIC = "learning-batch-size";

    @Test
    @DisplayName("큰 배치 (500개): 처리량 높지만 시간 오래 걸림")
    void largeBatch_HighThroughput() throws Exception {
        log.info("\n");
        log.info("========================================");
        log.info("실험 시작: 큰 배치 크기 (500개)");
        log.info("========================================");

        // 1. 메시지 전송
        log.info("\n=== 1단계: 메시지 1000개 전송 ===");
        produceMessages(1000);
        Thread.sleep(2000);

        // 2. Consumer (큰 배치)
        log.info("\n=== 2단계: Consumer 시작 ===");
        log.info("설정: max.poll.records=500");

        Properties props = createConsumerProps("large-batch-group");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "500");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        long startTime = System.currentTimeMillis();
        int totalProcessed = 0;
        int pollCount = 0;

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(TOPIC));

            while (totalProcessed < 1000) {
                pollCount++;
                long pollStart = System.currentTimeMillis();

                log.info("\n=== poll() #{} ===", pollCount);
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
                log.info("읽은 메시지 개수: {}", records.count());

                if (records.isEmpty()) {
                    break;
                }

                // 메시지 처리 시뮬레이션 (각 10ms)
                Thread.sleep(records.count() * 10L);

                totalProcessed += records.count();
                consumer.commitSync();

                long pollDuration = System.currentTimeMillis() - pollStart;
                log.info("poll #{} 처리 시간: {}ms, 누적: {}개", pollCount, pollDuration, totalProcessed);
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;

        log.info("\n========================================");
        log.info("결과: 큰 배치");
        log.info("========================================");
        log.info("- 배치 크기: 500개");
        log.info("- poll 횟수: {}회", pollCount);
        log.info("- 총 시간: {}ms", totalTime);
        log.info("- 초당 처리량: {:.2f}개/초", (totalProcessed * 1000.0) / totalTime);
        log.info("\n장점:");
        log.info("  ✅ poll 횟수 적음 (네트워크 오버헤드 감소)");
        log.info("  ✅ 처리량 높음");
        log.info("\n단점:");
        log.info("  ❌ poll당 시간 오래 걸림 (타임아웃 위험)");
        log.info("  ❌ 실패 시 재처리 범위 큼 (500개 전체)");
        log.info("  ❌ 메모리 사용 많음");
    }

    @Test
    @DisplayName("작은 배치 (10개): 안정적이지만 처리량 낮음")
    void smallBatch_Stable() throws Exception {
        log.info("\n");
        log.info("========================================");
        log.info("실험 시작: 작은 배치 크기 (10개)");
        log.info("========================================");

        // 1. 메시지 전송
        log.info("\n=== 1단계: 메시지 100개 전송 ===");
        produceMessages(100);
        Thread.sleep(2000);

        // 2. Consumer (작은 배치)
        log.info("\n=== 2단계: Consumer 시작 ===");
        log.info("설정: max.poll.records=10");

        Properties props = createConsumerProps("small-batch-group");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "10");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        long startTime = System.currentTimeMillis();
        int totalProcessed = 0;
        int pollCount = 0;

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(TOPIC));

            while (totalProcessed < 100) {
                pollCount++;
                long pollStart = System.currentTimeMillis();

                log.info("\n=== poll() #{} ===", pollCount);
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
                log.info("읽은 메시지 개수: {}", records.count());

                if (records.isEmpty()) {
                    break;
                }

                // 메시지 처리 시뮬레이션 (각 10ms)
                Thread.sleep(records.count() * 10L);

                totalProcessed += records.count();
                consumer.commitSync();

                long pollDuration = System.currentTimeMillis() - pollStart;
                log.info("poll #{} 처리 시간: {}ms, 누적: {}개", pollCount, pollDuration, totalProcessed);
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;

        log.info("\n========================================");
        log.info("결과: 작은 배치");
        log.info("========================================");
        log.info("- 배치 크기: 10개");
        log.info("- poll 횟수: {}회", pollCount);
        log.info("- 총 시간: {}ms", totalTime);
        log.info("- 초당 처리량: {:.2f}개/초", (totalProcessed * 1000.0) / totalTime);
        log.info("\n장점:");
        log.info("  ✅ poll당 시간 짧음 (타임아웃 안전)");
        log.info("  ✅ 실패 시 재처리 범위 작음 (10개만)");
        log.info("  ✅ 메모리 사용 적음");
        log.info("\n단점:");
        log.info("  ❌ poll 횟수 많음 (네트워크 오버헤드)");
        log.info("  ❌ 총 처리 시간 길 수 있음");
    }

    @Test
    @DisplayName("여러 배치 크기 비교")
    void compareBatchSizes() throws Exception {
        log.info("\n");
        log.info("========================================");
        log.info("실험 시작: 여러 배치 크기 비교");
        log.info("========================================");

        int[] batchSizes = {10, 50, 100, 200};

        log.info("\n배치 크기별 성능 비교:");
        log.info("메시지 1개당 처리 시간: 10ms");
        log.info("총 메시지: 1000개\n");

        for (int batchSize : batchSizes) {
            log.info("----------------------------------------");
            log.info("배치 크기: {}", batchSize);
            log.info("----------------------------------------");

            // 메시지 전송
            produceMessages(1000);
            Thread.sleep(1000);

            Properties props = createConsumerProps("compare-group-" + batchSize);
            props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.valueOf(batchSize));
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

            long startTime = System.currentTimeMillis();
            int totalProcessed = 0;
            int pollCount = 0;

            try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
                consumer.subscribe(Collections.singletonList(TOPIC));

                while (totalProcessed < 1000) {
                    pollCount++;
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));

                    if (records.isEmpty()) {
                        break;
                    }

                    Thread.sleep(records.count() * 10L);
                    totalProcessed += records.count();
                    consumer.commitSync();
                }
            }

            long totalTime = System.currentTimeMillis() - startTime;
            double avgTimePerPoll = (double) totalTime / pollCount;

            log.info("결과:");
            log.info("  - 총 처리: {}개", totalProcessed);
            log.info("  - poll 횟수: {}회", pollCount);
            log.info("  - 총 시간: {}ms", totalTime);
            log.info("  - poll당 평균: {:.2f}ms", avgTimePerPoll);
            log.info("  - 초당 처리량: {:.2f}개/초\n", (totalProcessed * 1000.0) / totalTime);

            Thread.sleep(2000);
        }

        log.info("========================================");
        log.info("학습 포인트");
        log.info("========================================");
        log.info("- 배치 크기는 처리 시간과 처리량의 트레이드오프");
        log.info("- 최적 값은 메시지 처리 시간에 따라 다름");
        log.info("- 공식: 배치 크기 × 처리 시간 < max.poll.interval.ms × 0.8");
    }

    @Test
    @DisplayName("배치 중 일부 실패 시 전체 재처리")
    void batchPartialFailure() throws Exception {
        log.info("\n");
        log.info("========================================");
        log.info("실험 시작: 배치 중 일부 실패");
        log.info("========================================");

        // 1. 메시지 전송
        log.info("\n=== 1단계: 메시지 50개 전송 ===");
        produceMessages(50);
        Thread.sleep(2000);

        // 2. Consumer (배치 20개)
        log.info("\n=== 2단계: Consumer 시작 (배치 20개) ===");

        Properties props = createConsumerProps("partial-failure-group");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "20");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        try {
            try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
                consumer.subscribe(Collections.singletonList(TOPIC));

                log.info("\n=== 첫 번째 poll() ===");
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
                log.info("읽은 메시지 개수: {}", records.count());

                int processedCount = 0;
                records.forEach(record -> {
                    log.info("처리: offset={}, value={}", record.offset(), record.value());
                });

                log.error("\n!!! 15번째 메시지에서 실패 발생! !!!");
                log.error("배치 20개 중 15개만 처리됨");
                log.error("커밋하지 않고 종료");

                // 커밋 안 함!
            }
        } catch (Exception e) {
            log.error("Consumer 종료");
        }

        // 3. 재시작
        log.info("\n=== 3단계: Consumer 재시작 ===");
        Thread.sleep(2000);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(TOPIC));

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            log.info("재시작 후 읽은 메시지 개수: {}", records.count());

            records.forEach(record -> {
                log.info("재처리: offset={}, value={}", record.offset(), record.value());
            });

            log.info("\n========================================");
            log.info("결과: 배치 전체 재처리!");
            log.info("========================================");
            log.info("- 첫 실행: 배치 20개 읽음, 15개 처리, 5개 미처리");
            log.info("- 커밋 안 됨 (일부만 처리)");
            log.info("- 재시작: 배치 20개 전체 다시 읽음");
            log.info("- 결과: 15개는 중복 처리됨!");
            log.info("\n학습:");
            log.info("  - 배치 처리는 All-or-Nothing");
            log.info("  - 일부만 처리하고 실패하면 전체 재처리");
            log.info("  - 배치 크기가 클수록 중복 처리 범위 큼");
            log.info("  - 멱등성이 중요한 이유!");
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
