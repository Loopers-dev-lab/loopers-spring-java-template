package com.loopers.domain.metrics; // Package changed

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "product_metrics")
@DynamicUpdate // 변경된 필드만 업데이트
public class ProductMetrics {

    @Id
    private Long productId;

    @Column(nullable = false)
    private Long likeCount;

    @Column(nullable = false)
    private Long salesCount;

    @Column(nullable = false)
    private Long viewCount;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_event_occurred_at")
    private LocalDateTime lastEventOccurredAt;

    @Builder
    public ProductMetrics(Long productId, Long likeCount, Long salesCount, Long viewCount) {
        this.productId = productId;
        this.likeCount = (likeCount != null) ? likeCount : 0L;
        this.salesCount = (salesCount != null) ? salesCount : 0L;
        this.viewCount = (viewCount != null) ? viewCount : 0L;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementLikeCount(long delta) {
        this.likeCount += delta;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementSalesCount(long quantity) {
        this.salesCount += quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementViewCount() {
        this.viewCount += 1;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 마지막 처리된 이벤트 시각 업데이트
     * 멱등성 보장을 위해 사용됩니다.
     */
    public void updateLastEventOccurredAt(LocalDateTime occurredAt) {
        this.lastEventOccurredAt = occurredAt;
    }
}