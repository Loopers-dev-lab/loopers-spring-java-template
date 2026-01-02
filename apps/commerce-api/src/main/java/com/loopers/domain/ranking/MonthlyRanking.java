package com.loopers.domain.ranking;

import com.loopers.infrastructure.converter.YearMonthAttributeConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.YearMonth;

@Entity
@Getter
@Table(name = "mv_product_rank_monthly")
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
}
