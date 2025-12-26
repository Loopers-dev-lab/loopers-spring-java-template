package com.loopers.confg.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

/**
 * Kafka Consumer Configuration
 */
@Configuration
public class KafkaConfig {

    public static final String BATCH_LISTENER = "batchListenerContainerFactory";

    /**
     * 배치 리스너 컨테이너 팩토리
     * - 한 번에 여러 메시지를 처리
     * - Manual Ack 모드
     */
    @Bean(name = BATCH_LISTENER)
    public ConcurrentKafkaListenerContainerFactory<String, String> batchListenerContainerFactory(
        ConsumerFactory<String, String> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);  // 배치 리스너 활성화
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);  // Manual Ack

        return factory;
    }
}
