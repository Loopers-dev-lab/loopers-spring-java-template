package com.loopers.batch.domain.ranking;

import com.loopers.infrastructure.converter.YearMonthAttributeConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.YearMonth;

@Entity
@Getter
@Table(
        name = "mv_product_rank_monthly",
        indexes = {
                @Index(name = "idx_monthPeriod", columnList = "month_period"),
                @Index(name = "idx_score", columnList = "score")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_monthly_rank", columnNames = {"product_id", "month_period"})
        }
)
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class MonthlyRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "monthly_rank")
    private int rank;

    private Long productId;

    private double score;

    @Convert(converter = YearMonthAttributeConverter.class)
    @Column(name = "month_period")
    private YearMonth monthPeriod;

    public static MonthlyRanking create(int rank, Long productId, double score, YearMonth yearMonth) {
        MonthlyRanking monthlyRanking = new MonthlyRanking();

        monthlyRanking.rank = rank;
        monthlyRanking.productId = productId;
        monthlyRanking.score = score;
        monthlyRanking.monthPeriod = yearMonth;

        return monthlyRanking;
    }
}
