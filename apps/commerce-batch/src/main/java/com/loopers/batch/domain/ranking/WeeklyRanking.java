package com.loopers.batch.domain.ranking;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Table(
        name = "mv_product_rank_weekly",
        indexes = {
                @Index(name = "idx_week", columnList = "week_start, week_end"),
                @Index(name = "idx_score", columnList = "score")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_weekly_rank", columnNames = {"product_id", "week_start", "week_end"})
        }
)
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class WeeklyRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "weekly_rank")
    private int rank;

    private Long productId;

    private double score;

    private LocalDate weekStart;

    private LocalDate weekEnd;

    public static WeeklyRanking create(int rank, Long productId, double score, LocalDate weekStart, LocalDate weekEnd) {
        WeeklyRanking weeklyRanking = new WeeklyRanking();

        weeklyRanking.rank = rank;
        weeklyRanking.productId = productId;
        weeklyRanking.score = score;
        weeklyRanking.weekStart = weekStart;
        weeklyRanking.weekEnd = weekEnd;

        return weeklyRanking;
    }
}
