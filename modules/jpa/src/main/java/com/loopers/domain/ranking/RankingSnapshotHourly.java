package com.loopers.domain.ranking;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 시간 단위 랭킹 스냅샷 엔티티
 */
@Entity
@Table(name = "ranking_snapshot_hourly", indexes = {
    @Index(name = "idx_snapshot_time_rank", columnList = "snapshot_time DESC, rank ASC"),
    @Index(name = "idx_product_id_snapshot_time", columnList = "product_id, snapshot_time"),
    @Index(name = "idx_snapshot_time", columnList = "snapshot_time")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RankingSnapshotHourly extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "rank", nullable = false)
    private Integer rank;

    @Column(name = "total_score", nullable = false)
    private Double totalScore;

    @Column(name = "snapshot_time", nullable = false)
    private LocalDateTime snapshotTime;

    @Builder
    private RankingSnapshotHourly(Long productId, Integer rank, Double totalScore, LocalDateTime snapshotTime) {
        this.productId = productId;
        this.rank = rank;
        this.totalScore = totalScore != null ? totalScore : 0.0;
        this.snapshotTime = snapshotTime;
        this.guard();
    }

    @Override
    protected void guard() {
        if (productId == null) {
            throw new IllegalArgumentException("productId는 필수입니다");
        }
        if (rank == null || rank < 1) {
            throw new IllegalArgumentException("rank는 1 이상이어야 합니다");
        }
        if (snapshotTime == null) {
            throw new IllegalArgumentException("snapshotTime은 필수입니다");
        }
    }
}

