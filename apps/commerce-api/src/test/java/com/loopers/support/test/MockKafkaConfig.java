package com.loopers.support.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

@TestConfiguration
public class MockKafkaConfig {

  @Bean
  @SuppressWarnings("unchecked")
  public KafkaTemplate<Object, Object> kafkaTemplate() {
    KafkaTemplate<Object, Object> mock = Mockito.mock(KafkaTemplate.class);
    when(mock.send(any(), any(), any())).thenReturn(new CompletableFuture<>());
    return mock;
  }
}