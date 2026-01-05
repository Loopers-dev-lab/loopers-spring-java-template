package com.loopers.interfaces.event.product;

import com.loopers.application.product.ProductFacade;
import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.domain.product.ProductEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@Component
public class ProductEventListener {

    private final ProductFacade productFacade;

    @KafkaListener(
            topics = {"payment.paid"},
            containerFactory = KafkaConfig.BATCH_LISTENER,
            groupId = "product"
    )
    public void handle(
            List<ConsumerRecord<Object,Object>> messages,
            Acknowledgment acknowledgment
    ){
//        messages.stream()
        acknowledgment.acknowledge();
    }

    @EventListener
    public void productViewOutboxHandle(ProductEvent.ProductViewed event) {
        productFacade.productViewOutboxHandle(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void productViewPublishKafka(ProductEvent.ProductViewed event) {
        productFacade.productViewPublishKafka(event);
    }
}
