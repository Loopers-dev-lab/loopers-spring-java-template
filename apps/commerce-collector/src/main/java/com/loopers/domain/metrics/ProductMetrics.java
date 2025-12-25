package com.loopers.domain.metrics;


import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_metrics")
@Getter
@NoArgsConstructor
public class ProductMetrics extends BaseEntity {
    @Column(nullable = false, unique = true)
    private Long productId;

    @Column(nullable = false)
    private Long likeCount = 0L;

    @Column(nullable = false)
    private Long orderCount = 0L;

    @Column(nullable = false)
    private Long viewCount = 0L;

    @Column(nullable = false)
    private Long totalOrderQuantity = 0L;

    @Builder
    public ProductMetrics(Long productId) {
        this.productId = productId;
        this.likeCount = 0L;
        this.orderCount = 0L;
        this.viewCount = 0L;
        this.totalOrderQuantity = 0L;
    }

    public static ProductMetrics create(Long productId) {
        return ProductMetrics.builder()
                .productId(productId)
                .build();
    }

    /**
     * 좋아요 수 증가
     */
    public void incrementLikeCount() {
        this.likeCount++;
    }

    /**
     * 좋아요 수 감소
     */
    public void decrementLikeCount() {
        if(this.likeCount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "좋아요를 감소시킬 수 없습니다");
        }
        this.likeCount--;
    }

    /**
     * 주문 수 증가
     */
    public void incrementOrderCount(int quantity) {
        this.orderCount++;

        if( quantity <= 0 ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 수량은 항상 양수여야 합니다");
        }

        this.totalOrderQuantity += quantity;
    }

    /**
     * 상품 조회 수 증가
     * */
    public void incrementViewCount() {
        this.viewCount++;
    }
}
