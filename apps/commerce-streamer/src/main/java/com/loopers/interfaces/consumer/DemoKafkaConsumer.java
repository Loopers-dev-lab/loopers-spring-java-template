package com.loopers.interfaces.consumer;

import com.loopers.config.kafka.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DemoKafkaConsumer {
    @KafkaListener(
        topics = {"${demo-kafka.test.topic-name}"},
        containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void demoListener(
        List<ConsumerRecord<Object,Object>> messages,
        Acknowledgment acknowledgment
    ){
        System.out.println(messages);
        acknowledgment.acknowledge();
    }
}
