package com.loopers.config.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.event.DeadLetterService;
import com.loopers.shared.event.DomainEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.BatchMessagingMessageConverter;
import org.springframework.kafka.support.converter.ByteArrayJsonMessageConverter;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConfig {
    public static final String BATCH_LISTENER = "BATCH_LISTENER_DEFAULT";
    public static final String SINGLE_LISTENER = "SINGLE_LISTENER_DEFAULT";

    public static final int MAX_POLLING_SIZE = 3000; // read 3000 msg
    public static final int FETCH_MIN_BYTES = (1024 * 1024); // 1mb
    public static final int FETCH_MAX_WAIT_MS = 5 * 1000; // broker waiting time = 5s
    public static final int SESSION_TIMEOUT_MS = 60 * 1000; // session timeout = 1m
    public static final int HEARTBEAT_INTERVAL_MS = 20 * 1000; // heartbeat interval = 20s ( 1/3 of session_timeout )
    public static final int MAX_POLL_INTERVAL_MS = 2 * 60 * 1000; // max poll interval = 2m
    public static final int MAX_RETRY_ATTEMPTS = 3; // 최대 재시도 횟수

    @Bean
    public ProducerFactory<Object, Object> producerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public ConsumerFactory<Object, Object> consumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<Object, Object> kafkaTemplate(ProducerFactory<Object, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    /**
     * StringSerializer용 KafkaTemplate
     * Outbox 패턴에서 이미 JSON String인 payload를 그대로 전송하기 위해 사용
     */
    @Bean
    public KafkaTemplate<String, String> stringKafkaTemplate(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        ProducerFactory<String, String> factory = new DefaultKafkaProducerFactory<>(props);
        return new KafkaTemplate<>(factory);
    }

    @Bean
    public ByteArrayJsonMessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new ByteArrayJsonMessageConverter(objectMapper);
    }

    /**
     * Dead Letter Queue를 위한 Error Handler
     * 최대 3회 재시도 후 DeadLetterService를 통해 DB에 저장합니다.
     */
    @Bean
    public CommonErrorHandler defaultErrorHandler(DeadLetterService deadLetterService, ObjectMapper objectMapper) {
        // FixedBackOff를 사용하여 정확한 재시도 횟수 제어
        FixedBackOff backOff = new FixedBackOff(1000L, MAX_RETRY_ATTEMPTS); // 1초 간격, 최대 3회

        return new DefaultErrorHandler((record, exception) -> {
            // 최종 실패 시 Dead Letter Queue에 저장
            // DefaultErrorHandler는 최대 재시도 횟수에 도달하면 이 recoverer를 호출함
            String payload = serializePayload(record.value(), objectMapper);
            String eventId = extractEventId(record.value());
            
            deadLetterService.saveFailedEvent(
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    eventId,
                    payload,
                    exception.getMessage(),
                    MAX_RETRY_ATTEMPTS
            );
        }, backOff);
    }

    @Bean(name = BATCH_LISTENER)
    public ConcurrentKafkaListenerContainerFactory<Object, Object> defaultBatchListenerContainerFactory(
            KafkaProperties kafkaProperties,
            ByteArrayJsonMessageConverter converter
    ) {
        Map<String, Object> consumerConfig = new HashMap<>(kafkaProperties.buildConsumerProperties());
        consumerConfig.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, MAX_POLLING_SIZE);
        consumerConfig.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, FETCH_MIN_BYTES);
        consumerConfig.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, FETCH_MAX_WAIT_MS);
        consumerConfig.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, SESSION_TIMEOUT_MS);
        consumerConfig.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, HEARTBEAT_INTERVAL_MS);
        consumerConfig.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, MAX_POLL_INTERVAL_MS);

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(consumerConfig));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL); // 수동 커밋
        factory.setBatchMessageConverter(new BatchMessagingMessageConverter(converter));
        factory.setConcurrency(3);
        factory.setBatchListener(true);
        return factory;
    }

    @Bean(name = SINGLE_LISTENER)
    public ConcurrentKafkaListenerContainerFactory<Object, Object> defaultSingleListenerContainerFactory(
            KafkaProperties kafkaProperties,
            ByteArrayJsonMessageConverter converter,
            DeadLetterService deadLetterService,
            ObjectMapper objectMapper
    ) {
        Map<String, Object> consumerConfig = new HashMap<>(kafkaProperties.buildConsumerProperties());
        consumerConfig.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, SESSION_TIMEOUT_MS);
        consumerConfig.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, HEARTBEAT_INTERVAL_MS);
        consumerConfig.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, MAX_POLL_INTERVAL_MS);

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(consumerConfig));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setRecordMessageConverter(converter); // Use Record converter
        factory.setCommonErrorHandler(defaultErrorHandler(deadLetterService, objectMapper)); // Error Handler 설정
        factory.setConcurrency(3);
        factory.setBatchListener(false);
        return factory;
    }

    /**
     * 페이로드를 JSON 문자열로 직렬화합니다.
     */
    private String serializePayload(Object value, ObjectMapper objectMapper) {
        try {
            if (value == null) {
                return "null";
            }
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return value != null ? value.toString() : "null";
        }
    }

    /**
     * 이벤트에서 eventId를 추출합니다.
     */
    private String extractEventId(Object value) {
        try {
            if (value instanceof DomainEvent) {
                return ((DomainEvent) value).getEventId();
            }
        } catch (Exception e) {
            // 무시
        }
        return null;
    }
}
