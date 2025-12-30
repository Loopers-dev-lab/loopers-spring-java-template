package com.loopers.config.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.like.event.LikeEvents;
import com.loopers.domain.order.event.OrderEvents;
import com.loopers.domain.payment.event.PaymentEvents;
import com.loopers.domain.product.event.ProductEvents;
import com.loopers.domain.stock.event.StockEvents;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.support.converter.ByteArrayJsonMessageConverter;
import org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper;
import org.springframework.kafka.support.mapping.Jackson2JavaTypeMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 메시지 타입 매핑 설정
 * 헤더의 __TypeId__ 값을 기반으로 적절한 Java 클래스로 자동 역직렬화
 */
@Configuration
@ConditionalOnProperty(name = "app.event.consumer.type", havingValue = "kafka", matchIfMissing = true)
public class KafkaTypeMapperConfig {

    /**
     * Kafka 메시지 타입 매핑을 위한 TypeMapper
     * 헤더의 __TypeId__ 값을 기반으로 적절한 Java 클래스로 역직렬화
     */
    @Bean
    public Jackson2JavaTypeMapper kafkaTypeMapper() {
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.TYPE_ID);
        
        // 이벤트 타입 매핑 설정
        Map<String, Class<?>> typeMappings = new HashMap<>();
        
        // ProductEvents
        typeMappings.put(ProductEvents.Created.class.getName(), ProductEvents.Created.class);
        typeMappings.put(ProductEvents.Updated.class.getName(), ProductEvents.Updated.class);
        typeMappings.put(ProductEvents.Deleted.class.getName(), ProductEvents.Deleted.class);
        
        // LikeEvents
        typeMappings.put(LikeEvents.ProductLikeSaved.class.getName(), LikeEvents.ProductLikeSaved.class);
        typeMappings.put(LikeEvents.ProductLikeDeleted.class.getName(), LikeEvents.ProductLikeDeleted.class);
        typeMappings.put(LikeEvents.LikeCountChanged.class.getName(), LikeEvents.LikeCountChanged.class);
        
        // OrderEvents
        typeMappings.put(OrderEvents.Created.class.getName(), OrderEvents.Created.class);
        
        // PaymentEvents
        typeMappings.put(PaymentEvents.CallbackReceived.class.getName(), PaymentEvents.CallbackReceived.class);
        typeMappings.put(PaymentEvents.Processed.class.getName(), PaymentEvents.Processed.class);
        typeMappings.put(PaymentEvents.ProcessingFailed.class.getName(), PaymentEvents.ProcessingFailed.class);
        
        // StockEvents
        typeMappings.put(StockEvents.Processed.class.getName(), StockEvents.Processed.class);
        typeMappings.put(StockEvents.ProcessingFailed.class.getName(), StockEvents.ProcessingFailed.class);
        
        // CouponEvents
        typeMappings.put(CouponEvents.Processed.class.getName(), CouponEvents.Processed.class);
        typeMappings.put(CouponEvents.ProcessingFailed.class.getName(), CouponEvents.ProcessingFailed.class);
        
        typeMapper.setIdClassMapping(typeMappings);
        return typeMapper;
    }

    /**
     * TypeMapper가 설정된 ByteArrayJsonMessageConverter
     * KafkaConfig의 기본 converter를 오버라이드하여 TypeMapper를 적용
     * @Primary로 설정하여 다른 모듈의 기본 converter보다 우선적으로 사용됨
     */
    @Bean(name = "jsonMessageConverter")
    @Primary
    public ByteArrayJsonMessageConverter jsonMessageConverterWithTypeMapper(
            ObjectMapper objectMapper,
            Jackson2JavaTypeMapper kafkaTypeMapper
    ) {
        ByteArrayJsonMessageConverter converter = new ByteArrayJsonMessageConverter(objectMapper);
        converter.setTypeMapper(kafkaTypeMapper);
        return converter;
    }
}

