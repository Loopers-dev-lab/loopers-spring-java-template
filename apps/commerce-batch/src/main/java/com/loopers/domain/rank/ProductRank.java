package com.loopers.domain.rank;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 상품 랭킹 Materialized View 엔티티.
 * <p>
 * 주간/월간 TOP 100 랭킹을 저장하는 조회 전용 테이블입니다.
 * </p>
 * <p>
 * <b>Materialized View 설계:</b>
 * <ul>
 *   <li>테이블: `mv_product_rank` (단일 테이블)</li>
 *   <li>주간 랭킹: period_type = WEEKLY</li>
 *   <li>월간 랭킹: period_type = MONTHLY</li>
 *   <li>TOP 100만 저장하여 조회 성능 최적화</li>
 * </ul>
 * </p>
 * <p>
 * <b>인덱스 전략:</b>
 * <ul>
 *   <li>복합 인덱스: (period_type, period_start_date, rank) - 기간별 랭킹 조회 최적화</li>
 *   <li>복합 인덱스: (period_type, period_start_date, product_id) - 특정 상품 랭킹 조회 최적화</li>
 * </ul>
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Entity
@Table(
    name = "mv_product_rank",
    indexes = {
        @Index(name = "idx_period_type_start_date_rank", columnList = "period_type, period_start_date, rank"),
        @Index(name = "idx_period_type_start_date_product_id", columnList = "period_type, period_start_date, product_id")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ProductRank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 기간 타입 (WEEKLY: 주간, MONTHLY: 월간)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 20)
    private PeriodType periodType;

    /**
     * 기간 시작일
     * <ul>
     *   <li>주간: 해당 주의 월요일 (ISO 8601 기준)</li>
     *   <li>월간: 해당 월의 1일</li>
     * </ul>
     */
    @Column(name = "period_start_date", nullable = false)
    private LocalDate periodStartDate;

    /**
     * 상품 ID
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * 랭킹 (1-100)
     */
    @Column(name = "rank", nullable = false)
    private Integer rank;

    /**
     * 좋아요 수
     */
    @Column(name = "like_count", nullable = false)
    private Long likeCount;

    /**
     * 판매량
     */
    @Column(name = "sales_count", nullable = false)
    private Long salesCount;

    /**
     * 조회 수
     */
    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    /**
     * 생성 시각
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 시각
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * ProductRank 인스턴스를 생성합니다.
     *
     * @param periodType 기간 타입 (WEEKLY 또는 MONTHLY)
     * @param periodStartDate 기간 시작일
     * @param productId 상품 ID
     * @param rank 랭킹 (1-100)
     * @param likeCount 좋아요 수
     * @param salesCount 판매량
     * @param viewCount 조회 수
     */
    public ProductRank(
        PeriodType periodType,
        LocalDate periodStartDate,
        Long productId,
        Integer rank,
        Long likeCount,
        Long salesCount,
        Long viewCount
    ) {
        this.periodType = periodType;
        this.periodStartDate = periodStartDate;
        this.productId = productId;
        this.rank = rank;
        this.likeCount = likeCount;
        this.salesCount = salesCount;
        this.viewCount = viewCount;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 랭킹 정보를 업데이트합니다.
     *
     * @param rank 새로운 랭킹
     * @param likeCount 좋아요 수
     * @param salesCount 판매량
     * @param viewCount 조회 수
     */
    public void updateRank(Integer rank, Long likeCount, Long salesCount, Long viewCount) {
        this.rank = rank;
        this.likeCount = likeCount;
        this.salesCount = salesCount;
        this.viewCount = viewCount;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 기간 타입 열거형.
     */
    public enum PeriodType {
        WEEKLY,  // 주간
        MONTHLY  // 월간
    }
}

