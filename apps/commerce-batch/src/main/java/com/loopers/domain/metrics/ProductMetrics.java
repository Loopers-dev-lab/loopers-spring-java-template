package com.loopers.domain.metrics;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 상품 메트릭 집계 엔티티.
 * <p>
 * Spring Batch에서 집계 및 조회를 위한 메트릭 엔티티입니다.
 * </p>
 * <p>
 * <b>도메인 분리 근거:</b>
 * <ul>
 *   <li>외부 시스템(데이터 플랫폼, 분석 시스템)을 위한 메트릭 집계</li>
 *   <li>Product 도메인의 핵심 비즈니스 로직과는 분리된 관심사</li>
 *   <li>Spring Batch를 통한 대량 데이터 처리</li>
 * </ul>
 * </p>
 * <p>
 * <b>모듈별 독립성:</b>
 * <ul>
 *   <li>commerce-batch 전용 엔티티 (독립적 진화 가능)</li>
 *   <li>commerce-streamer와는 별도로 관리되어 모듈별 커스터마이징 가능</li>
 * </ul>
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Entity
@Table(name = "product_metrics")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ProductMetrics {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "like_count", nullable = false)
    private Long likeCount;

    @Column(name = "sales_count", nullable = false)
    private Long salesCount;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * ProductMetrics 인스턴스를 생성합니다.
     *
     * @param productId 상품 ID
     */
    public ProductMetrics(Long productId) {
        this.productId = productId;
        this.likeCount = 0L;
        this.salesCount = 0L;
        this.viewCount = 0L;
        this.version = 0L;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 좋아요 수를 증가시킵니다.
     */
    public void incrementLikeCount() {
        this.likeCount++;
        this.version++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 좋아요 수를 감소시킵니다.
     */
    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
            this.version++;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 판매량을 증가시킵니다.
     *
     * @param quantity 판매 수량
     */
    public void incrementSalesCount(Integer quantity) {
        if (quantity != null && quantity > 0) {
            this.salesCount += quantity;
            this.version++;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 상세 페이지 조회 수를 증가시킵니다.
     */
    public void incrementViewCount() {
        this.viewCount++;
        this.version++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 이벤트의 버전을 기준으로 메트릭을 업데이트해야 하는지 확인합니다.
     * <p>
     * 이벤트의 `version`이 메트릭의 `version`보다 크면 업데이트합니다.
     * 이를 통해 오래된 이벤트가 최신 메트릭을 덮어쓰는 것을 방지합니다.
     * </p>
     *
     * @param eventVersion 이벤트의 버전
     * @return 업데이트해야 하면 true, 그렇지 않으면 false
     */
    public boolean shouldUpdate(Long eventVersion) {
        if (eventVersion == null) {
            // 이벤트에 버전 정보가 없으면 업데이트 (하위 호환성)
            return true;
        }
        // 이벤트 버전이 메트릭 버전보다 크면 업데이트
        return eventVersion > this.version;
    }
}

