package com.loopers.domain.ranking;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mv_product_rank_weekly",
    indexes = {
        @Index(name = "idx_weekly_period_rank", columnList = "period_yyyyww, ranking"),
        @Index(name = "idx_weekly_period", columnList = "period_yyyyww"),
        @Index(name = "idx_weekly_product", columnList = "productId")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_weekly_period_product", columnNames = {"period_yyyyww", "productId"}),
        @UniqueConstraint(name = "uk_weekly_period_rank", columnNames = {"period_yyyyww", "ranking"})
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MvProductRankWeekly extends BaseEntity {

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 6, name = "period_yyyyww")
    private String yearMonthWeek;

    @Column(nullable = false)
    private Integer ranking;

    @Column(nullable = false)
    private Double score;

    @Column(nullable = false)
    private Integer likeCount = 0;

    @Column(nullable = false)
    private Integer orderCount = 0;

    @Column(nullable = false)
    private Integer viewCount = 0;

    public MvProductRankWeekly(Long productId, String yearMonthWeek, Integer ranking, Double score, 
                              Integer likeCount, Integer orderCount, Integer viewCount) {
        this.productId = productId;
        this.yearMonthWeek = yearMonthWeek;
        this.ranking = ranking;
        this.score = score;
        this.likeCount = likeCount;
        this.orderCount = orderCount;
        this.viewCount = viewCount;
    }

    public static MvProductRankWeekly create(Long productId, String yearMonthWeek, Integer ranking,
                                           Integer likeCount, Integer orderCount, Integer viewCount) {
        final double VIEW_WEIGHT = 0.1;
        final double LIKE_WEIGHT = 0.2;
        final double ORDER_WEIGHT = 0.6;
        
        double score = (VIEW_WEIGHT * viewCount) + (LIKE_WEIGHT * likeCount) + (ORDER_WEIGHT * orderCount);
        
        return new MvProductRankWeekly(productId, yearMonthWeek, ranking, score, likeCount, orderCount, viewCount);
    }
}