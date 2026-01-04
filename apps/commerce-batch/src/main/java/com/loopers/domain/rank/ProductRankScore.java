package com.loopers.domain.rank;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 상품 랭킹 점수 집계 임시 엔티티.
 * <p>
 * Step 1 (집계 로직 계산)에서 사용하는 임시 테이블입니다.
 * product_id별로 점수를 집계하여 저장하며, 랭킹 번호는 저장하지 않습니다.
 * </p>
 * <p>
 * <b>사용 목적:</b>
 * <ul>
 *   <li>Step 1에서 모든 ProductMetrics를 읽어서 product_id별로 점수 집계</li>
 *   <li>Step 2에서 전체 데이터를 읽어서 TOP 100 선정 및 랭킹 번호 부여</li>
 * </ul>
 * </p>
 * <p>
 * <b>인덱스 전략:</b>
 * <ul>
 *   <li>product_id에 유니크 인덱스: 같은 product_id는 하나의 레코드만 존재 (UPSERT 방식)</li>
 *   <li>score에 인덱스: Step 2에서 정렬 시 성능 최적화</li>
 * </ul>
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Entity
@Table(
    name = "tmp_product_rank_score",
    indexes = {
        @Index(name = "idx_product_id", columnList = "product_id", unique = true),
        @Index(name = "idx_score", columnList = "score")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ProductRankScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 상품 ID
     */
    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    /**
     * 좋아요 수 (집계된 값)
     */
    @Column(name = "like_count", nullable = false)
    private Long likeCount;

    /**
     * 판매량 (집계된 값)
     */
    @Column(name = "sales_count", nullable = false)
    private Long salesCount;

    /**
     * 조회 수 (집계된 값)
     */
    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    /**
     * 종합 점수
     * <p>
     * 가중치:
     * <ul>
     *   <li>좋아요: 0.3</li>
     *   <li>판매량: 0.5</li>
     *   <li>조회수: 0.2</li>
     * </ul>
     * </p>
     */
    @Column(name = "score", nullable = false)
    private Double score;

    /**
     * 메트릭 값을 설정합니다.
     * <p>
     * Repository에서만 사용하는 내부 메서드입니다.
     * </p>
     */
    public void setMetrics(Long likeCount, Long salesCount, Long viewCount, Double score) {
        this.likeCount = likeCount;
        this.salesCount = salesCount;
        this.viewCount = viewCount;
        this.score = score;
        this.updatedAt = LocalDateTime.now();
    }

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
     * ProductRankScore 인스턴스를 생성합니다.
     *
     * @param productId 상품 ID
     * @param likeCount 좋아요 수
     * @param salesCount 판매량
     * @param viewCount 조회 수
     * @param score 종합 점수
     */
    public ProductRankScore(
        Long productId,
        Long likeCount,
        Long salesCount,
        Long viewCount,
        Double score
    ) {
        this.productId = productId;
        this.likeCount = likeCount;
        this.salesCount = salesCount;
        this.viewCount = viewCount;
        this.score = score;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

}

