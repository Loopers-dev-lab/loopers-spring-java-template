package com.loopers.applications.streamer.consumer.product;

import com.loopers.JacksonUtil;
import com.loopers.applications.streamer.consumer.product.dto.IncreaseProductLikeMetricEvent;
import com.loopers.core.infra.event.kafka.config.KafkaConfig;
import com.loopers.core.service.product.IncreaseProductLikeMetricService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class IncreaseProductLikeMetricKafkaConsumer {

    private final IncreaseProductLikeMetricService service;

    @KafkaListener(
            topics = {"${spring.kafka.topic.product-like}"},
            containerFactory = KafkaConfig.BATCH_LISTENER,
            groupId = "increase-product-like-count"
    )
    public void listen(
            List<ConsumerRecord<Object, String>> records,
            Acknowledgment acknowledgment
    ) {
        records.stream()
                .map(event -> JacksonUtil.convertToObject(event.value(), IncreaseProductLikeMetricEvent.class))
                .map(IncreaseProductLikeMetricEvent::toCommand)
                .forEach(service::increase);

        acknowledgment.acknowledge();
    }
}
