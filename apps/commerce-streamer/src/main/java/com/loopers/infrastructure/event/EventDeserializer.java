package com.loopers.infrastructure.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.infrastructure.event.payloads.LikeActionPayloadV1;
import com.loopers.infrastructure.event.payloads.PaymentSuccessPayloadV1;
import com.loopers.infrastructure.event.payloads.ProductViewPayloadV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 이벤트 역직렬화 유틸리티
 *
 * @author hyunjikoh
 * @since 2025. 12. 18.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventDeserializer {
    
    private final ObjectMapper objectMapper;
    
    public DomainEventEnvelope deserializeEnvelope(Object kafkaValue) {
        try {
            if (kafkaValue instanceof String json) {
                return objectMapper.readValue(json, DomainEventEnvelope.class);
            }
            // Map 형태로 온 경우 JSON으로 변환 후 역직렬화
            String json = objectMapper.writeValueAsString(kafkaValue);
            return objectMapper.readValue(json, DomainEventEnvelope.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize event envelope: {}", kafkaValue, e);
            return null;
        }
    }
    
    public ProductViewPayloadV1 deserializeProductView(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, ProductViewPayloadV1.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize ProductViewPayloadV1: {}", payloadJson, e);
            return null;
        }
    }
    
    public LikeActionPayloadV1 deserializeLikeAction(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, LikeActionPayloadV1.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize LikeActionPayloadV1: {}", payloadJson, e);
            return null;
        }
    }
    
    public PaymentSuccessPayloadV1 deserializePaymentSuccess(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, PaymentSuccessPayloadV1.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize PaymentSuccessPayloadV1: {}", payloadJson, e);
            return null;
        }
    }
}