package com.loopers.domain.ranking;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 연 단위 랭킹 스냅샷 엔티티
 */
@Entity
@Table(name = "ranking_snapshot_yearly", indexes = {
    @Index(name = "idx_product_id_snapshot_time", columnList = "product_id, snapshot_time"),
    @Index(name = "idx_snapshot_time", columnList = "snapshot_time")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RankingSnapshotYearly extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "total_score", nullable = false)
    private Double totalScore;

    @Column(name = "snapshot_time", nullable = false)
    private LocalDateTime snapshotTime;

    @Builder
    private RankingSnapshotYearly(Long productId, Double totalScore, LocalDateTime snapshotTime) {
        this.productId = productId;
        this.totalScore = totalScore != null ? totalScore : 0.0;
        this.snapshotTime = snapshotTime;
        this.guard();
    }

    @Override
    protected void guard() {
        if (productId == null) {
            throw new IllegalArgumentException("productId는 필수입니다");
        }
        if (snapshotTime == null) {
            throw new IllegalArgumentException("snapshotTime은 필수입니다");
        }
    }
}

