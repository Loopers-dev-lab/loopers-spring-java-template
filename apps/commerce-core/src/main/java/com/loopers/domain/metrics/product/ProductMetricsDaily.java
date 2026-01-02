package com.loopers.domain.metrics.product;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;

@Entity
@Table(
    name = "tb_product_metrics_daily",
    indexes = {
        @Index(name = "idx_product_metrics_daily_product_id", columnList = "product_id"),
        @Index(name = "idx_product_metrics_daily_date", columnList = "date"),
        @Index(name = "idx_product_metrics_daily_product_date", columnList = "product_id,date")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_product_metrics_daily_product_date",
            columnNames = {"product_id", "date"}
        )
    }
)
@Getter
public class ProductMetricsDaily extends BaseEntity {
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "date", nullable = false)
    private LocalDate date; // yyyy-MM-dd 형식
    
    @Column(name = "like_count", nullable = false)
    private Integer likeCount;
    
    @Column(name = "view_count", nullable = false)
    private Long viewCount;
    
    @Column(name = "sold_count", nullable = false)
    private Long soldCount;
    
    protected ProductMetricsDaily() {
    }
    
    public static ProductMetricsDaily create(
        Long productId, 
        LocalDate date
    ) {
        if (productId == null || productId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 1 이상이어야 합니다.");
        }
        if (date == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "날짜는 필수입니다.");
        }
        
        ProductMetricsDaily daily = new ProductMetricsDaily();
        daily.productId = productId;
        daily.date = date;
        daily.likeCount = 0;
        daily.viewCount = 0L;
        daily.soldCount = 0L;
        return daily;
    }
    
    public void incrementLikeCount() {
        this.likeCount += 1;
    }
    
    public void decrementLikeCount() {
        if (this.likeCount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "좋아요 수는 0 미만으로 내려갈 수 없습니다.");
        }
        this.likeCount -= 1;
    }
    
    public void incrementViewCount() {
        if (this.viewCount == null) {
            this.viewCount = 0L;
        }
        this.viewCount += 1;
    }
    
    public void incrementSoldCount(Long quantity) {
        if (this.soldCount == null) {
            this.soldCount = 0L;
        }
        if (quantity == null || quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "판매 수량은 1 이상이어야 합니다.");
        }
        this.soldCount += quantity;
    }
}


