package com.loopers.domain.rank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 롤링 7일 주간 랭킹 MV 엔티티.
 * period_start = 기준일(anchorDate), 윈도우는 [anchorDate-6, anchorDate].
 */
@Entity
@Table(
	name = "mv_product_rank_weekly",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_weekly_product_period",
		columnNames = {"product_id", "period_start"}
	),
	indexes = {
		@Index(name = "idx_weekly_period_rank", columnList = "period_start, rank_position"),
		@Index(name = "idx_weekly_period_score", columnList = "period_start, total_score DESC")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyProductRank {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "period_start", nullable = false)
	private LocalDate periodStart;

	@Column(name = "rank_position", nullable = false)
	private Integer rankPosition;

	@Column(name = "total_score", nullable = false)
	private Double totalScore;

	@Column(name = "like_count", nullable = false)
	private Integer likeCount;

	@Column(name = "view_count", nullable = false)
	private Integer viewCount;

	@Column(name = "order_count", nullable = false)
	private Integer orderCount;

	@Column(name = "sales_amount", nullable = false, precision = 15, scale = 2)
	private BigDecimal salesAmount;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Builder
	public WeeklyProductRank(
		Long productId,
		LocalDate periodStart,
		Integer rankPosition,
		Double totalScore,
		Integer likeCount,
		Integer viewCount,
		Integer orderCount,
		BigDecimal salesAmount
	) {
		this.productId = productId;
		this.periodStart = periodStart;
		this.rankPosition = rankPosition;
		this.totalScore = totalScore;
		this.likeCount = likeCount;
		this.viewCount = viewCount;
		this.orderCount = orderCount;
		this.salesAmount = salesAmount;
	}

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = this.createdAt;
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
}
