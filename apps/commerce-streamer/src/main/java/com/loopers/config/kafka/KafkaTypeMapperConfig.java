package com.loopers.config.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.like.event.LikeEvents;
import com.loopers.domain.product.event.ProductEvents;
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
 * Kafka 메시지 타입 매핑 설정 (commerce-streamer용)
 * 랭킹 시스템에서 사용하는 이벤트 타입만 매핑
 */
@Configuration
@ConditionalOnProperty(name = "app.event.consumer.type", havingValue = "kafka", matchIfMissing = true)
public class KafkaTypeMapperConfig {

    @Bean
    public Jackson2JavaTypeMapper kafkaTypeMapper() {
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.TYPE_ID);
        
        Map<String, Class<?>> typeMappings = new HashMap<>();
        
        // LikeEvents
        typeMappings.put(LikeEvents.ProductLikeSaved.class.getName(), LikeEvents.ProductLikeSaved.class);
        typeMappings.put(LikeEvents.ProductLikeDeleted.class.getName(), LikeEvents.ProductLikeDeleted.class);
        typeMappings.put(LikeEvents.LikeCountChanged.class.getName(), LikeEvents.LikeCountChanged.class);
        
        // ProductEvents
        typeMappings.put(ProductEvents.Viewed.class.getName(), ProductEvents.Viewed.class);
        
        // OrderEvents는 동적 타입 체크로 처리 (commerce-api 모듈에만 존재)
        // typeMappings.put("com.loopers.domain.order.event.OrderEvents$Created", ...);
        
        typeMapper.setIdClassMapping(typeMappings);
        return typeMapper;
    }

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



