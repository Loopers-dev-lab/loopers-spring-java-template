package com.loopers.domain.metrics.repository;

/**
 * 메트릭 업데이트를 위한 Repository 인터페이스
 * <p>
 * 동시성 안전한 메트릭 업데이트 작업을 담당합니다.
 *
 * @author hyunjikoh
 * @since 2025. 12. 19.
 */
public interface MetricsRepository {
    
    /**
     * 조회수 증가
     */
    void incrementView(Long productId, long occurredAtEpochMillis);
    
    /**
     * 좋아요 수 변경 (증가/감소)
     */
    void applyLikeDelta(Long productId, int delta, long occurredAtEpochMillis);
    
    /**
     * 판매량 증가
     */
    void addSales(Long productId, int quantity, long occurredAtEpochMillis);
    
    /**
     * 재고 소진 이벤트 처리 (캐시 무효화 중심)
     */
    void handleStockDepleted(Long productId, Long brandId, long occurredAtEpochMillis);
}
