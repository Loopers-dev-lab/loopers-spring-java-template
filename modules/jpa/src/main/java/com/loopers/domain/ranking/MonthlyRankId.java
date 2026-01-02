package com.loopers.domain.ranking;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 월간 랭킹 복합 PK
 * - product_id + year_month 조합으로 유일성 보장
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class MonthlyRankId implements Serializable {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "base_year_month", nullable = false, length = 7)
    private String yearMonth;  // e.g., "2024-12"

    private MonthlyRankId(Long productId, String yearMonth) {
        this.productId = productId;
        this.yearMonth = yearMonth;
    }

    public static MonthlyRankId of(Long productId, String yearMonth) {
        return new MonthlyRankId(productId, yearMonth);
    }
}
