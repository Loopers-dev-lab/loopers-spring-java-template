package com.loopers.domain.ranking;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Entity
@Table(
        name = "mv_product_rank_weekly",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"product_id", "period_start_date", "period_end_date"})
        }
)
@Getter
public class MvProductRankWeekly extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = true)
    private Integer ranking;

    @Column(nullable = false)
    private Double score;

    @Column(name = "period_start_date", nullable = false)
    private LocalDate periodStartDate;

    @Column(name = "period_end_date", nullable = false)
    private LocalDate periodEndDate;

    @Column(name = "like_count", nullable = false)
    private Long likeCount;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "sales_count", nullable = false)
    private Long salesCount;

    @Builder
    private MvProductRankWeekly(Long productId, Integer ranking, Double score, LocalDate periodStartDate, LocalDate periodEndDate,
                                Long likeCount, Long viewCount, Long salesCount) {
        this.productId = productId;
        this.ranking = ranking;
        this.score = score;
        this.periodStartDate = periodStartDate;
        this.periodEndDate = periodEndDate;
        this.likeCount = likeCount;
        this.viewCount = viewCount;
        this.salesCount = salesCount;
    }

    public MvProductRankWeekly() {
    }

    public static MvProductRankWeekly create(Long productId, Integer ranking, Double score, LocalDate periodStartDate,
                                             LocalDate periodEndDate, Long likeCount, Long viewCount, Long salesCount) {
        return MvProductRankWeekly.builder()
                .productId(productId)
                .ranking(ranking)
                .score(score)
                .periodStartDate(periodStartDate)
                .periodEndDate(periodEndDate)
                .likeCount(likeCount)
                .viewCount(viewCount)
                .salesCount(salesCount)
                .build();
    }

    public void update(Integer ranking, Double score, Long likeCount, Long viewCount, Long salesCount) {
        this.ranking = ranking;
        this.score = score;
        this.likeCount = likeCount;
        this.viewCount = viewCount;
        this.salesCount = salesCount;
    }

    public void updateMetrics(Double score, Long likeCount, Long viewCount, Long salesCount) {
        this.score = score;
        this.likeCount = likeCount;
        this.viewCount = viewCount;
        this.salesCount = salesCount;
    }

    public void updateRanking(Integer ranking) {
        this.ranking = ranking;
    }
}


