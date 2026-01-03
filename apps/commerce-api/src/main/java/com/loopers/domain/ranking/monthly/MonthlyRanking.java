package com.loopers.domain.ranking.monthly;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
    name = "mv_product_rank_monthly",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_monthly_ranking_month_position",
            columnNames = {"month_start_date", "rank_position"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlyRanking extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "rank_position", nullable = false)
    private Integer rankPosition;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "total_score", nullable = false)
    private Double totalScore;
    
    @Column(name = "month_year", nullable = false)
    private String monthYear;
    
    @Column(name = "month_start_date", nullable = false)
    private LocalDate monthStartDate;
    
    @Column(name = "month_end_date", nullable = false)
    private LocalDate monthEndDate;
    
    public MonthlyRanking(Integer rankPosition, Long productId, Double totalScore,
                         String monthYear, LocalDate monthStartDate, LocalDate monthEndDate) {
        this.rankPosition = rankPosition;
        this.productId = productId;
        this.totalScore = totalScore;
        this.monthYear = monthYear;
        this.monthStartDate = monthStartDate;
        this.monthEndDate = monthEndDate;
    }
}