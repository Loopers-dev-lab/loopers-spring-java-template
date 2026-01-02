package com.loopers.domain.ranking;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@Entity
@Table(name = "tb_mv_product_rank_weekly", indexes = {
        @Index(name = "idx_mv_product_rank_weekly_ranking_date", columnList = "ranking_date"),
        @Index(name = "idx_mv_product_rank_weekly_product_date", columnList = "product_id,ranking_date"),
        @Index(name = "idx_mv_product_rank_weekly_date_ranking", columnList = "ranking_date,ranking")
})
@Getter
@Setter
public class MvProductRankWeekly extends BaseEntity {
    private Long productId;
    private String productName;
    private Long brandId;
    private String brandName;
    private Double score;
    private Integer likeCount;
    private Integer viewCount;
    private Integer orderCount;
    private Integer ranking;
    private ZonedDateTime rankingDate;
}
