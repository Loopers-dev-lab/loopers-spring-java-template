package com.loopers.domain.ranking;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "mv_product_rank_monthly")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MvProductRankMonthly {

    @EmbeddedId
    private ProductRankId id;

    @Column(name = "like_count", nullable = false)
    private long likeCount;

    @Column(name = "sales_count", nullable = false)
    private long salesCount;

    @Column(name = "score", nullable = false)
    private double score;

    @Column(name = "rank", nullable = false)
    private int rank;

    @Column(name = "aggregated_at", nullable = false)
    private LocalDateTime aggregatedAt;

    private MvProductRankMonthly(
            ProductRankId id,
            long likeCount,
            long salesCount,
            double score,
            int rank,
            LocalDateTime aggregatedAt
    ) {
        this.id = id;
        this.likeCount = likeCount;
        this.salesCount = salesCount;
        this.score = score;
        this.rank = rank;
        this.aggregatedAt = aggregatedAt;
    }

    public static MvProductRankMonthly create(
            String yearMonth,
            Long productId,
            long likeCount,
            long salesCount,
            double score,
            int rank,
            LocalDateTime aggregatedAt
    ) {
        return new MvProductRankMonthly(
                ProductRankId.of(yearMonth, productId),
                likeCount,
                salesCount,
                score,
                rank,
                aggregatedAt
        );
    }

    public String getPeriodKey() {
        return id != null ? id.getPeriodKey() : null;
    }

    public Long getProductId() {
        return id != null ? id.getProductId() : null;
    }
}
