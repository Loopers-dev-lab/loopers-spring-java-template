package com.loopers.support.test;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;

@TestConfiguration
public class MockKafkaConfig {

  @Bean
  @Primary
  @SuppressWarnings("unchecked")
  public KafkaTemplate<String, String> kafkaTemplate() {
    return Mockito.mock(KafkaTemplate.class);
  }
}