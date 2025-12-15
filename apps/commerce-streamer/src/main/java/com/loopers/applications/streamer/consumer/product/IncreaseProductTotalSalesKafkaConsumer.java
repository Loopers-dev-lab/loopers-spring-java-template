package com.loopers.applications.streamer.consumer.product;

import com.loopers.JacksonUtil;
import com.loopers.applications.streamer.consumer.product.dto.IncreaseProductTotalSalesEvent;
import com.loopers.core.infra.event.kafka.config.KafkaConfig;
import com.loopers.core.service.product.IncreaseProductTotalSalesService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class IncreaseProductTotalSalesKafkaConsumer {

    private final IncreaseProductTotalSalesService service;

    @KafkaListener(
            topics = {"${spring.kafka.topic.payment-completed}"},
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void listen(
            List<ConsumerRecord<Object, String>> records,
            Acknowledgment acknowledgment
    ) {
        records.stream()
                .map(event -> JacksonUtil.convertToObject(event.value(), IncreaseProductTotalSalesEvent.class))
                .map(IncreaseProductTotalSalesEvent::toCommand)
                .forEach(service::increase);
        acknowledgment.acknowledge();
    }
}
