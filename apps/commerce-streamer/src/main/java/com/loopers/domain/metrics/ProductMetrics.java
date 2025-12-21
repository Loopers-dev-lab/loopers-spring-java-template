package com.loopers.domain.metrics;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_metrics", 
       indexes = {
           @Index(name = "idx_product_bucket", columnList = "productId, bucketTime"),
           @Index(name = "idx_bucket_time", columnList = "bucketTime")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMetrics extends BaseEntity {

    @Column(nullable = false)
    private Long productId;
    
    @Column(nullable = false)
    private LocalDateTime bucketTime;

    @Column(nullable = false)
    private Long likeCount = 0L;

    @Column(nullable = false)
    private Long salesCount = 0L;

    @Column(nullable = false)
    private Long viewCount = 0L;

    public ProductMetrics(Long productId, LocalDateTime bucketTime) {
        this.productId = productId;
        this.bucketTime = bucketTime;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void incrementSalesCount(Long quantity) {
        this.salesCount += quantity;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }
}