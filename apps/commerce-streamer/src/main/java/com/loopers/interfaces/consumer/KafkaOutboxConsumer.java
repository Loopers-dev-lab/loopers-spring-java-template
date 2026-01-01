package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.domain.OutboxEvent;
import com.loopers.domain.ProductEventType;
import com.loopers.domain.ProductMetric;
import com.loopers.domain.ProductMetricRepository;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KafkaOutboxConsumer {
    private final ProductMetricRepository productMetricRepository;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.BASIC_ISO_DATE;

    @KafkaListener(
            topics = {"product-viewed", "product-liked", "product-sales"},
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    @Transactional
    public void productViewedListener(
            List<ConsumerRecord<String, String>> messages,
            Acknowledgment acknowledgment
    ) throws JsonProcessingException {

        Map<Long, Double> scoreDelta = new HashMap<>();

        String date = LocalDate.now(KST).format(YYYYMMDD);

        for (var record : messages) {
            OutboxEvent value = objectMapper.readValue(record.value(), OutboxEvent.class);
            Long productId = value.aggregateId();
            ProductEventType eventType = ProductEventType.valueOf(value.eventType());

            scoreDelta.merge(productId, weight(eventType), Double::sum);

            ProductMetric productMetric = productMetricRepository.findByProductIdAndDate(productId, date);

            if (productMetric == null) {
                ProductMetric newProductMetric = ProductMetric.of(productId, date, eventType);

                productMetricRepository.save(newProductMetric);
            }
            else {
                productMetric.increaseProductMetric(eventType);
            }
        }

        String key = "ranking:all:" + date;
        ZSetOperations<String, String> zset = redisTemplate.opsForZSet();

        for (var e : scoreDelta.entrySet()) {
            zset.incrementScore(key, String.valueOf(e.getKey()), e.getValue()); // ZINCRBY
        }

        redisTemplate.expire(key, Duration.ofDays(2));

        acknowledgment.acknowledge();
    }

    double weight(ProductEventType eventType) {
        return switch (eventType) {
            case PRODUCT_VIEWED -> 0.1;
            case PRODUCT_LIKED -> 0.3;
            case PRODUCT_SALES -> 0.6;
        };
    }
}

