package com.loopers.applications.streamer.consumer.product;

import com.loopers.JacksonUtil;
import com.loopers.applications.streamer.consumer.product.dto.IncreaseProductLikeRankingScoreEvent;
import com.loopers.core.infra.event.kafka.config.KafkaConfig;
import com.loopers.core.service.productlike.IncreaseProductLikeRankingScoreService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class IncreaseProductLikeRankingScoreKafkaConsumer {

    private final IncreaseProductLikeRankingScoreService service;

    @KafkaListener(
            topics = {"${spring.kafka.topic.product-like}"},
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void listen(
            List<ConsumerRecord<Object, String>> records,
            Acknowledgment acknowledgment
    ) {
        records.stream()
                .map(event -> JacksonUtil.convertToObject(event.value(), IncreaseProductLikeRankingScoreEvent.class))
                .map(IncreaseProductLikeRankingScoreEvent::toCommand)
                .forEach(service::increase);
        acknowledgment.acknowledge();
    }
}
