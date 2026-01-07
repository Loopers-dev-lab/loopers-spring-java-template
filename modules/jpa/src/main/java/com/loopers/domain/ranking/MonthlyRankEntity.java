package com.loopers.domain.ranking;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 월간 랭킹 MV 엔티티
 * - 배치 Job에서 월간 TOP 100 랭킹을 저장
 */
@Entity
@Table(
    name = "mv_product_rank_monthly",
    indexes = {
        @Index(name = "idx_year_month_rank", columnList = "base_year_month, base_rank_position")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlyRankEntity {

    @EmbeddedId
    private MonthlyRankId id;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "like_count", nullable = false)
    private long likeCount;

    @Column(name = "sales_count", nullable = false)
    private long salesCount;

    @Column(name = "order_count", nullable = false)
    private long orderCount;

    @Column(name = "total_score", nullable = false)
    private long totalScore;

    @Column(name = "base_rank_position", nullable = false)
    private int rankPosition;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private MonthlyRankEntity(MonthlyRankId id, long viewCount, long likeCount,
                              long salesCount, long orderCount, long totalScore, int rankPosition) {
        this.id = id;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.salesCount = salesCount;
        this.orderCount = orderCount;
        this.totalScore = totalScore;
        this.rankPosition = rankPosition;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 월간 랭킹 엔티티를 생성합니다.
     */
    public static MonthlyRankEntity create(Long productId, String yearMonth,
                                           long viewCount, long likeCount,
                                           long salesCount, long orderCount,
                                           long totalScore, int rankPosition) {
        Objects.requireNonNull(productId, "상품 ID는 필수입니다.");
        Objects.requireNonNull(yearMonth, "월 정보는 필수입니다.");
        validateRankPosition(rankPosition);

        MonthlyRankId id = MonthlyRankId.of(productId, yearMonth);
        return new MonthlyRankEntity(id, viewCount, likeCount, salesCount, orderCount, totalScore, rankPosition);
    }

    private static void validateRankPosition(int rankPosition) {
        if (rankPosition < 1 || rankPosition > 100) {
            throw new IllegalArgumentException(
                String.format("순위는 1~100 범위여야 합니다. (입력값: %d)", rankPosition));
        }
    }

    // 편의 메서드
    public Long getProductId() {
        return id.getProductId();
    }

    public String getYearMonth() {
        return id.getYearMonth();
    }
}
