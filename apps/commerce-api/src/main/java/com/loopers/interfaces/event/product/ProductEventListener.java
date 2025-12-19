package com.loopers.interfaces.event.product;

import com.loopers.confg.kafka.KafkaConfig;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class ProductEventListener {

    @KafkaListener(
            topics = {"payment.paid"},
            containerFactory = KafkaConfig.BATCH_LISTENER,
            groupId = "product"
    )
    public void productPaidListener(
            List<ConsumerRecord<Object,Object>> messages,
            Acknowledgment acknowledgment
    ){
//        messages.stream()
        acknowledgment.acknowledge();
    }
}
