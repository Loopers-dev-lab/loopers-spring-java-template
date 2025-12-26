package com.loopers.application.ranking;

import com.loopers.domain.ranking.RankingEventLog;
import com.loopers.domain.ranking.RankingEventLogRepository;
import com.loopers.domain.ranking.RankingEventType;
import com.loopers.shared.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 랭킹 이벤트 로그 서비스
 * 이벤트 로깅 및 멱등성 관리를 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingEventLogService {

    private final RankingEventLogRepository rankingEventLogRepository;

    /**
     * 이벤트가 이미 처리되었는지 확인 (멱등성 체크)
     */
    @Transactional(readOnly = true)
    public boolean isAlreadyProcessed(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        return rankingEventLogRepository.findByEventId(eventId).isPresent();
    }

    /**
     * 랭킹 이벤트 로그 저장 (동기 처리)
     * 
     * @param eventId 이벤트 ID (멱등성 보장용)
     * @param productId 상품 ID
     * @param eventType 이벤트 타입
     * @param score 점수
     * @param occurredAt 발생 시각
     * @return 저장된 RankingEventLog
     */
    @Transactional
    public RankingEventLog saveEventLog(String eventId, Long productId, RankingEventType eventType, Double score, LocalDateTime occurredAt) {
        RankingEventLog eventLog = RankingEventLog.builder()
                .eventId(eventId)
                .productId(productId)
                .eventType(eventType)
                .score(score)
                .occurredAt(occurredAt)
                .build();
        
        return rankingEventLogRepository.save(eventLog);
    }

    /**
     * DomainEvent에서 eventId 추출
     */
    public String extractEventId(DomainEvent event) {
        if (event == null) {
            return null;
        }
        try {
            return event.getEventId();
        } catch (Exception e) {
            log.warn("이벤트에서 eventId 추출 실패: {}", e.getMessage());
            return null;
        }
    }
}

