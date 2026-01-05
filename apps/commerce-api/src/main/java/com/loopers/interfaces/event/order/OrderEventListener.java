package com.loopers.interfaces.event.order;

import com.loopers.confg.kafka.KafkaConfig;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    @KafkaListener(
            topics = {"payment.paid"},
            containerFactory = KafkaConfig.BATCH_LISTENER,
            groupId = "order"
    )
    public void orderPaidListener(
            List<ConsumerRecord<Object,Object>> messages,
            Acknowledgment acknowledgment
    ){
//        messages.stream()
        acknowledgment.acknowledge();
    }
}
