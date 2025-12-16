package com.loopers.interfaces.consumer;

import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.loopers.confg.kafka.KafkaConfig;

@Component
public class DemoKafkaConsumer {
    @KafkaListener(
            topics = {"${demo-kafka.test.topic-name}"},
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void demoListener(
            List<ConsumerRecord<Object, Object>> messages,
            Acknowledgment acknowledgment
    ) {
        System.out.println(messages);
        acknowledgment.acknowledge();
    }
}
