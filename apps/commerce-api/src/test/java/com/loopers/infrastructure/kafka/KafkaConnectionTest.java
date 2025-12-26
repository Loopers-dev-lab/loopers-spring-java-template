package com.loopers.infrastructure.kafka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    classes = {com.loopers.confg.kafka.KafkaConfig.class},
    properties = {
        "spring.kafka.bootstrap-servers=localhost:19092",
        "spring.kafka.producer.acks=all",
        "spring.kafka.producer.properties.enable.idempotence=true",
        "spring.kafka.producer.retries=3",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"
    }
)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class KafkaConnectionTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("Kafka Producer 연결 테스트")
    void kafkaProducerConnectionTest() throws Exception {
        // given
        String topic = "demo.internal.topic-v1";
        String key = "test-key";
        String message = "Hello Kafka!";

        // when
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(topic, key, message);

        // then
        SendResult<String, Object> result = future.get(10, TimeUnit.SECONDS);
        assertThat(result).isNotNull();
        assertThat(result.getRecordMetadata().topic()).isEqualTo(topic);

        System.out.println("✅ Kafka Producer 연결 성공!");
        System.out.println("Topic: " + result.getRecordMetadata().topic());
        System.out.println("Partition: " + result.getRecordMetadata().partition());
        System.out.println("Offset: " + result.getRecordMetadata().offset());
    }

    @Test
    @DisplayName("Kafka Producer acks=all 설정 확인")
    void kafkaProducerAcksConfigTest() {
        // given
        var producerFactory = kafkaTemplate.getProducerFactory();
        var configs = producerFactory.getConfigurationProperties();

        // then
        assertThat(configs.get("acks")).isEqualTo("all");
        assertThat(Boolean.parseBoolean(String.valueOf(configs.get("enable.idempotence")))).isTrue();

        System.out.println("✅ Producer 설정 확인:");
        System.out.println("acks: " + configs.get("acks"));
        System.out.println("enable.idempotence: " + configs.get("enable.idempotence"));
        System.out.println("retries: " + configs.get("retries"));
    }
}
