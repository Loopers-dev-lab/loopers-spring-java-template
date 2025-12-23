package com.loopers.applications.streamer.consumer.product;

import com.loopers.JacksonUtil;
import com.loopers.applications.streamer.consumer.product.dto.IncreaseProductSalesRankingScoreEvent;
import com.loopers.core.infra.event.kafka.config.KafkaConfig;
import com.loopers.core.service.product.IncreaseProductSalesRankingScoreService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class IncreaseProductSalesRankingScoreKafkaConsumer {

    private final IncreaseProductSalesRankingScoreService service;

    @KafkaListener(
            topics = {"${spring.kafka.topic.payment-completed}"},
            containerFactory = KafkaConfig.BATCH_LISTENER,
            groupId = "increase-product-sales-ranking-score"
    )
    public void listen(
            List<ConsumerRecord<Object, String>> records,
            Acknowledgment acknowledgment
    ) {
        records.stream()
                .map(event -> JacksonUtil.convertToObject(event.value(), IncreaseProductSalesRankingScoreEvent.class))
                .map(IncreaseProductSalesRankingScoreEvent::toCommand)
                .forEach(service::increase);
        acknowledgment.acknowledge();
    }
}
