package com.loopers.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * 테스트 환경 설정
 * - Kafka 자동 설정 비활성화
 * - JPA, Redis만 활성화
 */
@TestConfiguration
@EnableAutoConfiguration(exclude = {KafkaAutoConfiguration.class})
@ComponentScan(basePackages = "com.loopers")
public class TestConfig {
}
