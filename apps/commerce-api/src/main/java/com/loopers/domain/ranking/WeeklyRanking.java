package com.loopers.domain.ranking;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Table(name = "mv_product_rank_weekly")
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
}
