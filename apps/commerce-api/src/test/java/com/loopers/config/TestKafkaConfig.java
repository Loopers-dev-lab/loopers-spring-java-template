package com.loopers.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;

/**
 * 테스트 환경에서 Kafka Mock 빈을 제공하는 설정 클래스
 */
@TestConfiguration
@Profile("test")
public class TestKafkaConfig {

    @Bean
    @Primary
    public KafkaTemplate<Object, Object> kafkaTemplate() {
        return mock(KafkaTemplate.class);
    }
}
