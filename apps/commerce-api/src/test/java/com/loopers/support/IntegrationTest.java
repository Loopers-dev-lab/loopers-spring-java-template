package com.loopers.support;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public abstract class IntegrationTest {

    @MockitoBean
    protected KafkaTemplate<String, String> kafkaTemplate;
}
