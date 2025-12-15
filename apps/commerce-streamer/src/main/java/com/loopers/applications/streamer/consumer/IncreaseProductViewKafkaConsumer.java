package com.loopers.applications.streamer.consumer;

import com.loopers.JacksonUtil;
import com.loopers.applications.streamer.consumer.dto.IncreaseProductViewEvent;
import com.loopers.core.infra.event.kafka.config.KafkaConfig;
import com.loopers.core.service.product.IncreaseProductMetricViewCountService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class IncreaseProductViewKafkaConsumer {

    private final IncreaseProductMetricViewCountService service;

    @KafkaListener(
            topics = {"${spring.kafka.topic.product-detail-viewed}"},
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void listen(
            List<ConsumerRecord<Object, String>> records,
            Acknowledgment acknowledgment
    ) {
        records.stream()
                .map(event -> JacksonUtil.convertToObject(event.value(), IncreaseProductViewEvent.class))
                .map(IncreaseProductViewEvent::toCommand)
                .forEach(service::increase);

        acknowledgment.acknowledge();
    }
}
