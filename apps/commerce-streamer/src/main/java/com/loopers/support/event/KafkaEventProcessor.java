package com.loopers.support.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.event.EventHandled;
import com.loopers.domain.event.EventHandledRepository;
import com.loopers.messaging.event.KafkaEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventProcessor {

    private final ObjectMapper objectMapper;
    private final EventHandledRepository eventHandledRepository;


    public <T> void process(Consumer<KafkaEventMessage<T>> handler, String json) throws JsonProcessingException {
        KafkaEventMessage<T> message =
                objectMapper.readValue(
                        json,
                        new TypeReference<KafkaEventMessage<T>>() {}
                );

        UUID eventId = message.getEventId();

        if (eventHandledRepository.existsByEventId(eventId)) {
            log.info("이미 처리된 이벤트입니다. eventId={}", eventId);
            return;
        }

        handler.accept(message);

        eventHandledRepository.save(new EventHandled(eventId));
    }
}
