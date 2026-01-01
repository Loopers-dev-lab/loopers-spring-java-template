package com.loopers.domain.ranking;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.ZonedDateTime;

@Entity
@Table(name = "tb_mv_product_rank_weekly")
@Getter
public class MvProductRankWeekly extends BaseEntity {
    private Long productId;
    private String productName;
    private Long brandId;
    private String brandName;
    private Double score;
    private Integer likeCount;
    private Integer viewCount;
    private Integer orderCount;
    private Integer rank;
    private ZonedDateTime rankingDate;
}
