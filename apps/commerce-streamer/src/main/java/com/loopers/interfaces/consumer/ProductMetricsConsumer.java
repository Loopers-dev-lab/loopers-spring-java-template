package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.metrics.ProductMetricsCommand;
import com.loopers.application.metrics.ProductMetricsFacade;
import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.interfaces.dto.ProductLikePayload;
import com.loopers.interfaces.dto.ProductStockPayload;
import com.loopers.interfaces.dto.ProductViewPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMetricsConsumer {

    private final ProductMetricsFacade productMetricsFacade;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.product-like-name}")
    private String productLikeTopic;

    @Value("${kafka.topic.product-stock-name}")
    private String productStockTopic;

    @Value("${kafka.topic.product-view-name}")
    private String productViewTopic;

    @KafkaListener(
            topics = {"${kafka.topic.product-like-name}", "${kafka.topic.product-stock-name}", "${kafka.topic.product-view-name}"},
            groupId = "${kafka.consumer.product-metrics-group}",
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void listen(
            List<ConsumerRecord<String, String>> records,
            Acknowledgment acknowledgment
    ) {
        try {
            for (ConsumerRecord<String, String> record : records) {
                String payload = record.value();
                String topic = record.topic();

                if (topic == null) {
                    log.warn("Received null topic for payload: {}", payload);
                    continue;
                }

                try {
                    processPayload(topic, payload);
                } catch (Exception e) {
                    log.error("메트릭 처리 실패: topic={}, payload={}", topic, payload, e);
                    // 개별 메시지 실패는 로깅 후 계속 진행
                }
            }
        } finally {
            // 모든 메시지 처리 후 manual ack
            acknowledgment.acknowledge();
        }
    }

    private void processPayload(String topic, String payload) throws JsonProcessingException {
        Set<String> allowedTopics = Set.of(productLikeTopic, productStockTopic, productViewTopic);

        if (!allowedTopics.contains(topic)) {
            log.warn("허용되지 않은 토픽: {}", topic);
            return;
        }

        if (topic.contains("product-like")) {
            ProductLikePayload likePayload = objectMapper.readValue(payload, ProductLikePayload.class);
            ProductMetricsCommand likeCommand = ProductMetricsCommand.from(likePayload);
            productMetricsFacade.processLikeMetrics(likeCommand);

        } else if (topic.contains("product-stock")) {
            ProductStockPayload stockPayload = objectMapper.readValue(payload, ProductStockPayload.class);
            ProductMetricsCommand stockCommand = ProductMetricsCommand.from(stockPayload);
            productMetricsFacade.processStockMetrics(stockCommand);

        } else if (topic.contains("product-view")) {
            ProductViewPayload viewPayload = objectMapper.readValue(payload, ProductViewPayload.class);
            ProductMetricsCommand viewCommand = ProductMetricsCommand.from(viewPayload);
            productMetricsFacade.processViewMetrics(viewCommand);
        }
    }
}
