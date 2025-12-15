package com.loopers.core.service.config;

import com.loopers.JacksonUtil;
import com.loopers.core.domain.event.EventInbox;
import com.loopers.core.domain.event.repository.EventInboxRepository;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.event.vo.AggregateId;
import com.loopers.core.domain.event.vo.EventId;
import com.loopers.core.domain.event.vo.EventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class EventInboxAspect {

    private final EventInboxRepository eventInboxRepository;

    @Around("@annotation(com.loopers.core.service.config.InboxEvent)")
    public Object handleEventInbox(ProceedingJoinPoint joinPoint) throws Throwable {
        InboxEvent annotation = getInboxEventAnnotation(joinPoint);
        Object commandObject = joinPoint.getArgs()[0];

        String eventIdValue = extractEventId(commandObject, annotation.eventIdField());
        String aggregateId = extractAggregateId(commandObject, annotation.aggregateIdField());
        String payloadJson = JacksonUtil.convertToString(commandObject);

        EventId eventId = new EventId(eventIdValue);
        EventType eventType = EventType.valueOf(annotation.eventType());
        AggregateType aggregateType = AggregateType.valueOf(annotation.aggregateType());

        // 중복 확인: eventId로 이미 처리된 이벤트인지 확인
        if (eventInboxRepository.findByEventId(eventId).isPresent()) {
            log.info("이미 처리된 이벤트입니다. eventId={}, eventType={}", eventId.value(), eventType.name());
            return null;
        }

        try {
            // 비즈니스 로직 처리
            Object result = joinPoint.proceed();

            // Inbox에 저장 (감시 목적)
            EventInbox eventInbox = EventInbox.create(
                    eventId,
                    aggregateType,
                    new AggregateId(aggregateId),
                    eventType,
                    new EventPayload(payloadJson)
            );
            eventInboxRepository.save(eventInbox.process());

            log.info("이벤트가 정상 처리되었습니다. eventId={}, eventType={}", eventIdValue, eventType);
            return result;
        } catch (Exception e) {
            // 처리 실패 시 FAILED 상태로 저장
            EventInbox failedInbox = EventInbox.create(
                    eventId,
                    aggregateType,
                    new AggregateId(aggregateId),
                    eventType,
                    new EventPayload(payloadJson)
            );
            eventInboxRepository.save(failedInbox.fail());

            log.error("이벤트 처리 중 오류가 발생했습니다. eventId={}, eventType={}", eventIdValue, eventType, e);
            throw new RuntimeException("이벤트 처리 중 오류 발생", e);
        }
    }

    private String extractEventId(Object commandObject, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = commandObject.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        Object fieldValue = field.get(commandObject);

        // EventId 같은 record 객체인 경우 value() 메서드 호출
        if (fieldValue != null && fieldValue.getClass().isRecord()) {
            try {
                Object value = fieldValue.getClass().getMethod("value").invoke(fieldValue);
                return value.toString();
            } catch (Exception e) {
                return fieldValue.toString();
            }
        }

        return fieldValue != null ? fieldValue.toString() : "";
    }

    private String extractAggregateId(Object commandObject, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = commandObject.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        Object fieldValue = field.get(commandObject);

        // ProductId 같은 record 객체인 경우 value() 메서드 호출
        if (fieldValue != null && fieldValue.getClass().isRecord()) {
            try {
                Object value = fieldValue.getClass().getMethod("value").invoke(fieldValue);
                return value.toString();
            } catch (Exception e) {
                return fieldValue.toString();
            }
        }

        return fieldValue != null ? fieldValue.toString() : "";
    }

    private InboxEvent getInboxEventAnnotation(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        String methodName = joinPoint.getSignature().getName();
        Class<?>[] parameterTypes = new Class[joinPoint.getArgs().length];

        for (int i = 0; i < joinPoint.getArgs().length; i++) {
            parameterTypes[i] = joinPoint.getArgs()[i].getClass();
        }

        return joinPoint.getTarget()
                .getClass()
                .getMethod(methodName, parameterTypes)
                .getAnnotation(InboxEvent.class);
    }
}
