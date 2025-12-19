package com.loopers.event.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.event.OutboxEvent;
import com.loopers.domain.event.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 1000) // 1초마다 실행
    @Transactional
    public void publishPendingEvents() {
        // 최근 생성된 이벤트부터 처리 (약간의 딜레이를 주어 트랜잭션 커밋 완료 보장)
        LocalDateTime beforeTime = LocalDateTime.now().minusSeconds(1); 
        Pageable pageable = PageRequest.of(0, 50); // 한 번에 50개씩 처리

        List<OutboxEvent> pendingEvents = outboxEventRepository.findAllPendingEventsBefore(beforeTime, pageable);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox events", pendingEvents.size());

        for (OutboxEvent outboxEvent : pendingEvents) {
            try {
                // 페이로드 역직렬화 필요 없이 String 그대로 전송하거나, 다시 객체로 변환해서 전송
                // KafkaTemplate이 Object를 받으므로, 여기서는 String payload를 그대로 보내지 않고
                // Consumer가 JSON을 기대한다면 StringSerializer를 쓰거나, 여기서 객체로 변환해야 함.
                // 기존 KafkaConfig는 ValueSerializer가 JsonSerializer임.
                // 따라서 객체로 변환해서 보내야 JsonSerializer가 다시 JSON으로 변환함 (비효율적일 수 있음).
                // 혹은 KafkaTemplate<String, String>을 별도로 주입받아 보낼 수도 있음.
                // 간단하게 구현하기 위해 여기서 다시 객체로 변환하지 않고, 
                // KafkaTemplate의 ValueSerializer 설정을 확인. -> JsonSerializer임.
                // 이미 JSON String인 payload를 다시 JSON으로 감싸는 문제가 생길 수 있음.
                
                // 해결책: StringSerializer를 사용하는 KafkaTemplate을 쓰거나, 
                // payload를 Object로 역직렬화해서 보내야 함.
                // 여기서는 안전하게 Object로 역직렬화 (타입 정보가 없으므로 Map이나 JsonNode로 변환하거나, 그냥 String 전송)
                
                // 가장 깔끔한 방법: payload는 이미 JSON String이므로, 이를 그대로 바이트로 보내거나 String으로 보냄.
                // 하지만 KafkaConfig의 default template은 JsonSerializer를 사용 중.
                // 임시로 Object로 변환하지 않고 String 그대로 보낼 경우, 
                // Consumer는 "String" 타입의 JSON을 받게 됨 (이중 인코딩 가능성).
                
                // => KafkaTemplate을 <String, String> 타입으로 하나 더 정의해서 쓰는 게 가장 좋음.
                // 하지만 KafkaConfig 수정 없이 가려면, 
                // 1. payload를 Map으로 변환해서 전송
                // 2. 그냥 보냄 (Consumer가 String을 파싱)
                
                // 여기서는 "값" 자체를 String으로 전송하도록 처리 (KafkaConfig의 JsonSerializer가 String도 처리 가능)
                // 단, JsonSerializer는 String을 받으면 JSON String으로 만듦 -> "\"payload\"" 형태가 됨.
                // 따라서 올바른 JSON 전송을 위해선 실제 객체로 변환하거나, StringSerializer를 써야 함.
                
                // 일단 Map으로 변환해서 전송 시도
                Object eventObject = objectMapper.readValue(outboxEvent.getPayload(), Object.class);
                
                kafkaTemplate.send(outboxEvent.getTopic(), outboxEvent.getAggregateId(), eventObject)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish outbox event: {}", outboxEvent.getId(), ex);
                            // 비동기 콜백 내에서는 트랜잭션 처리가 까다로움. 
                            // 여기서는 동기적으로 send().get()을 하거나, 
                            // 별도 트랜잭션으로 상태 업데이트를 해야 함.
                            // 하지만 @Transactional 메서드 내이므로 DB 업데이트는 메서드 종료 시 커밋됨.
                            // send()가 비동기이므로, 메서드가 끝나버리면 커밋됨.
                            // 따라서 "발행 성공 여부"를 확실히 하려면 동기 전송을 하거나,
                            // 콜백에서 별도 서비스 호출로 상태 업데이트를 해야 함.
                            
                            // 간단한 구현: 동기 전송 (get)
                        }
                    }).get(); // 동기 대기

                outboxEvent.markAsPublished();
                
            } catch (Exception e) {
                log.error("Error processing outbox event: {}", outboxEvent.getId(), e);
                outboxEvent.markAsFailed();
            }
        }
    }
}

