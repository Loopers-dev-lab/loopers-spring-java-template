package com.loopers.applications.streamer.consumer.product;

import com.loopers.JacksonUtil;
import com.loopers.applications.streamer.consumer.product.dto.IncreaseProductViewRankingScoreEvent;
import com.loopers.core.infra.event.kafka.config.KafkaConfig;
import com.loopers.core.service.product.IncreaseProductViewRankingScoreService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class IncreaseProductViewRankingScoreKafkaConsumer {

    private final IncreaseProductViewRankingScoreService service;

    @KafkaListener(
            topics = {"${spring.kafka.topic.product-detail-viewed}"},
            containerFactory = KafkaConfig.BATCH_LISTENER,
            groupId = "increase-product-view-ranking-score"
    )
    public void listen(
            List<ConsumerRecord<Object, String>> records,
            Acknowledgment acknowledgment
    ) {
        records.stream()
                .map(event -> JacksonUtil.convertToObject(event.value(), IncreaseProductViewRankingScoreEvent.class))
                .map(IncreaseProductViewRankingScoreEvent::toCommand)
                .forEach(service::increase);

        acknowledgment.acknowledge();
    }
}
