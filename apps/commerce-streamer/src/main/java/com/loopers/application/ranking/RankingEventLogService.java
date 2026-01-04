package com.loopers.application.ranking;

import com.loopers.domain.ranking.RankingEventLog;
import com.loopers.domain.ranking.RankingEventLogRepository;
import com.loopers.domain.ranking.RankingEventType;
import com.loopers.shared.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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
        return saveEventLog(eventId, productId, eventType, score, occurredAt, null, null);
    }

    /**
     * 랭킹 이벤트 로그 저장 (Raw 데이터 포함, ORDER 이벤트용)
     * 
     * @param eventId 이벤트 ID (멱등성 보장용)
     * @param productId 상품 ID
     * @param eventType 이벤트 타입
     * @param score 점수
     * @param occurredAt 발생 시각
     * @param rawPrice Raw 가격 데이터 (ORDER 이벤트의 경우, null 가능)
     * @param rawQuantity Raw 수량 데이터 (ORDER 이벤트의 경우, null 가능)
     * @return 저장된 RankingEventLog
     */
    @Transactional
    public RankingEventLog saveEventLog(String eventId, Long productId, RankingEventType eventType, Double score, 
                                       LocalDateTime occurredAt, BigDecimal rawPrice, Integer rawQuantity) {
        RankingEventLog eventLog = RankingEventLog.builder()
                .eventId(eventId)
                .productId(productId)
                .eventType(eventType)
                .score(score)
                .occurredAt(occurredAt)
                .rawPrice(rawPrice)
                .rawQuantity(rawQuantity)
                .build();
        
        return rankingEventLogRepository.save(eventLog);
    }

    /**
     * 여러 이벤트 ID에 대해 이미 처리된 이벤트 ID 목록을 조회 (배치 멱등성 체크)
     * 
     * @param eventIds 조회할 이벤트 ID 목록
     * @return 이미 처리된 이벤트 ID Set
     */
    @Transactional(readOnly = true)
    public Set<String> getProcessedEventIds(List<String> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Set.of();
        }
        // null이거나 빈 문자열인 eventId 필터링
        List<String> validEventIds = eventIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .toList();
        
        if (validEventIds.isEmpty()) {
            return Set.of();
        }
        
        return rankingEventLogRepository.findAllEventIdsByEventIdIn(validEventIds);
    }

    /**
     * 여러 랭킹 이벤트 로그를 한 번에 저장 (배치 저장)
     * 
     * @param eventLogs 저장할 이벤트 로그 목록
     * @return 저장된 이벤트 로그 목록
     */
    @Transactional
    public List<RankingEventLog> saveEventLogs(List<RankingEventLog> eventLogs) {
        if (eventLogs == null || eventLogs.isEmpty()) {
            return List.of();
        }
        return rankingEventLogRepository.saveAll(eventLogs);
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

