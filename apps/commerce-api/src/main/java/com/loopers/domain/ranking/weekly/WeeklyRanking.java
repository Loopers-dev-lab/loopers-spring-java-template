package com.loopers.domain.ranking.weekly;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
    name = "mv_product_rank_weekly",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_weekly_ranking_week_position",
            columnNames = {"week_start_date", "rank_position"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyRanking extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "rank_position", nullable = false)
    private Integer rankPosition;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "total_score", nullable = false)
    private Double totalScore;
    
    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;
    
    @Column(name = "week_end_date", nullable = false) 
    private LocalDate weekEndDate;
    
    public WeeklyRanking(Integer rankPosition, Long productId, Double totalScore, 
                        LocalDate weekStartDate, LocalDate weekEndDate) {
        this.rankPosition = rankPosition;
        this.productId = productId;
        this.totalScore = totalScore;
        this.weekStartDate = weekStartDate;
        this.weekEndDate = weekEndDate;
    }
}