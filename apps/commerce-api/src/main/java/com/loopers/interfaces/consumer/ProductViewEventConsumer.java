package com.loopers.interfaces.consumer;

import com.loopers.config.kafka.KafkaConfig;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.event.ProductEvents;
import com.loopers.domain.product.view.ProductView;
import com.loopers.domain.product.view.ProductViewRepository;
import com.loopers.event.consumer.KafkaMessageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(
        topics = {"product.created.v1", "product.updated.v1", "product.deleted.v1"},
        groupId = "commerce-api-productview-group",
        containerFactory = KafkaConfig.SINGLE_LISTENER
)
public class ProductViewEventConsumer {

    private final KafkaMessageProcessor messageProcessor;
    private final ProductViewRepository productViewRepository;
    private final BrandRepository brandRepository;

    @KafkaHandler
    @Transactional
    public void handleCreated(ConsumerRecord<String, ProductEvents.Created> record, Acknowledgment ack) {
        log.info("ProductViewEventConsumer: ProductEvents.Created 수신 - productId: {}", 
                record.value().productId());

        messageProcessor.execute(record, ack, "productview", event -> {
            // Brand 조회하여 brandName 가져오기
            String brandName = brandRepository.findById(event.brandId())
                    .map(Brand::getName)
                    .orElse(null);

            // ProductView 생성
            ProductView productView = ProductView.builder()
                    .id(event.productId())
                    .name(event.name())
                    .price(event.price())
                    .brandId(event.brandId())
                    .brandName(brandName)
                    .status(event.status())
                    .likeCount(0L)
                    .createdAt(ZonedDateTime.of(event.getOccurredAt(), ZoneId.systemDefault()))
                    .build();

            productViewRepository.save(productView);
            log.debug("ProductView 생성 완료 - productId: {}", event.productId());
        });
    }

    @KafkaHandler
    @Transactional
    public void handleUpdated(ConsumerRecord<String, ProductEvents.Updated> record, Acknowledgment ack) {
        log.info("ProductViewEventConsumer: ProductEvents.Updated 수신 - productId: {}", 
                record.value().productId());

        messageProcessor.execute(record, ack, "productview", event -> {
            // Brand 조회하여 brandName 가져오기
            String brandName = brandRepository.findById(event.brandId())
                    .map(Brand::getName)
                    .orElse(null);

            // ProductView 업데이트
            productViewRepository.update(
                    event.productId(),
                    event.name(),
                    event.price(),
                    event.brandId(),
                    brandName,
                    event.status()
            );
            log.debug("ProductView 업데이트 완료 - productId: {}", event.productId());
        });
    }

    @KafkaHandler
    @Transactional
    public void handleDeleted(ConsumerRecord<String, ProductEvents.Deleted> record, Acknowledgment ack) {
        log.info("ProductViewEventConsumer: ProductEvents.Deleted 수신 - productId: {}", 
                record.value().productId());

        messageProcessor.execute(record, ack, "productview", event -> {
            productViewRepository.deleteById(event.productId());
            log.debug("ProductView 삭제 완료 - productId: {}", event.productId());
        });
    }

    @KafkaHandler(isDefault = true)
    public void handleDefault(ConsumerRecord<Object, Object> record, Acknowledgment ack) {
        log.warn("Received unknown event in product topics: {}", record.value());
        ack.acknowledge();
    }
}

