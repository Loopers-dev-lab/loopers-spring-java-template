package com.loopers.domain.ranking;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.loopers.cache.RankingRedisService;
import com.loopers.cache.dto.CachePayloads.RankingItem;
import com.loopers.cache.dto.CachePayloads.RankingScore;
import com.loopers.infrastructure.event.DomainEventEnvelope;
import com.loopers.infrastructure.event.EventDeserializer;
import com.loopers.infrastructure.event.payloads.LikeActionPayloadV1;
import com.loopers.infrastructure.event.payloads.PaymentSuccessPayloadV1;
import com.loopers.infrastructure.event.payloads.ProductViewPayloadV1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 랭킹 도메인 서비스
 * <p>
 * 랭킹 점수 생성, 배치 업데이트, 조회 등의 비즈니스 로직을 담당
 *
 * @author hyunjikoh
 * @since 2025.12.23
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RankingService {
    
    private final RankingRedisService rankingRedisService;
    private final EventDeserializer eventDeserializer;
    
    /**
     * 이벤트로부터 랭킹 점수 생성
     * 
     * @param envelope 도메인 이벤트 엔벨로프
     * @return 랭킹 점수 (생성되지 않으면 null)
     */
    public RankingScore generateRankingScore(DomainEventEnvelope envelope) {
        if (envelope == null || envelope.eventType() == null) {
            return null;
        }
        
        return switch (envelope.eventType()) {
            case "PRODUCT_VIEW" -> generateProductViewScore(envelope);
            case "LIKE_ACTION" -> generateLikeActionScore(envelope);
            case "PAYMENT_SUCCESS" -> generatePaymentSuccessScore(envelope);
            default -> {
                log.debug("랭킹 점수 생성 불가 - 지원하지 않는 이벤트 타입: {}", envelope.eventType());
                yield null;
            }
        };
    }
    
    /**
     * 배치로 랭킹 점수 업데이트
     * 
     * @param rankingScores 랭킹 점수 리스트
     * @param targetDate 대상 날짜 (null이면 오늘)
     */
    public void updateRankingScoresBatch(List<RankingScore> rankingScores, LocalDate targetDate) {
        if (rankingScores == null || rankingScores.isEmpty()) {
            log.debug("업데이트할 랭킹 점수가 없음");
            return;
        }
        
        LocalDate date = targetDate != null ? targetDate : LocalDate.now();
        
        try {
            rankingRedisService.updateRankingScoresBatch(rankingScores, date);
            log.debug("랭킹 점수 배치 업데이트 완료: {} scores, date: {}", rankingScores.size(), date);
        } catch (Exception e) {
            log.error("랭킹 점수 배치 업데이트 실패: date={}, scores={}", date, rankingScores.size(), e);
            throw new RankingUpdateException("랭킹 점수 업데이트 실패", e);
        }
    }
    
    /**
     * 랭킹 조회 (페이징)
     * 
     * @param date 날짜 (null이면 오늘)
     * @param page 페이지 (1부터 시작)
     * @param size 페이지 크기
     * @return 랭킹 리스트
     */
    public List<RankingItem> getRanking(LocalDate date, int page, int size) {
        if (page < 1 || size < 1) {
            throw new IllegalArgumentException("페이지와 크기는 1 이상이어야 합니다");
        }
        
        LocalDate targetDate = date != null ? date : LocalDate.now();
        
        try {
            return rankingRedisService.getRanking(targetDate, page, size);
        } catch (Exception e) {
            log.error("랭킹 조회 실패: date={}, page={}, size={}", targetDate, page, size, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 특정 상품의 랭킹 조회
     * 
     * @param productId 상품 ID
     * @param date 날짜 (null이면 오늘)
     * @return 랭킹 정보 (없으면 null)
     */
    public RankingItem getProductRanking(Long productId, LocalDate date) {
        if (productId == null) {
            return null;
        }
        
        LocalDate targetDate = date != null ? date : LocalDate.now();
        
        try {
            return rankingRedisService.getProductRanking(targetDate, productId);
        } catch (Exception e) {
            log.error("상품 랭킹 조회 실패: productId={}, date={}", productId, targetDate, e);
            return null;
        }
    }
    
    /**
     * 랭킹 데이터 존재 여부 확인
     */
    public boolean hasRankingData(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return rankingRedisService.hasRankingData(targetDate);
    }
    
    // ========== Private Methods ==========
    
    private RankingScore generateProductViewScore(DomainEventEnvelope envelope) {
        ProductViewPayloadV1 payload = eventDeserializer.deserializeProductView(envelope.payloadJson());
        if (payload == null || payload.productId() == null) {
            log.warn("상품 조회 이벤트 페이로드 오류: {}", envelope.payloadJson());
            return null;
        }
        
        return RankingScore.forProductView(payload.productId(), envelope.occurredAtEpochMillis());
    }
    
    private RankingScore generateLikeActionScore(DomainEventEnvelope envelope) {
        LikeActionPayloadV1 payload = eventDeserializer.deserializeLikeAction(envelope.payloadJson());
        if (payload == null || payload.productId() == null || payload.action() == null) {
            log.warn("좋아요 이벤트 페이로드 오류: {}", envelope.payloadJson());
            return null;
        }
        
        // 좋아요만 점수에 반영, 좋아요 취소는 반영하지 않음
        if ("LIKE".equals(payload.action())) {
            return RankingScore.forLikeAction(payload.productId(), envelope.occurredAtEpochMillis());
        }
        
        return null;
    }
    
    private RankingScore generatePaymentSuccessScore(DomainEventEnvelope envelope) {
        PaymentSuccessPayloadV1 payload = eventDeserializer.deserializePaymentSuccess(envelope.payloadJson());
        if (payload == null || payload.productId() == null || payload.totalPrice() == null) {
            log.warn("결제 성공 이벤트 페이로드 오류: {}", envelope.payloadJson());
            return null;
        }
        
        return RankingScore.forPaymentSuccess(
            payload.productId(), 
            payload.totalPrice(), 
            envelope.occurredAtEpochMillis()
        );
    }
    
    /**
     * 랭킹 업데이트 예외
     */
    public static class RankingUpdateException extends RuntimeException {
        public RankingUpdateException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
