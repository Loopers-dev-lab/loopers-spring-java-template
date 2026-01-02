package com.loopers.domain.ranking;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "mv_product_rank_monthly")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MonthlyRankingMv extends BaseEntity {
    @Column(name = "start_date", nullable = false)
    private String startDate;

    @Column(name = "end_date", nullable = false)
    private String endDate;

    @Column(name = "ref_product_id", nullable = false)
    private Long productId;

    @Column(name = "view_count_sum", nullable = false)
    private Long viewCountSum;

    @Column(name = "like_count_sum", nullable = false)
    private Long likeCountSum;

    @Column(name = "sales_volume_sum", nullable = false)
    private Long salesVolumeSum;

    @Column(nullable = false)
    private BigDecimal score;
}
