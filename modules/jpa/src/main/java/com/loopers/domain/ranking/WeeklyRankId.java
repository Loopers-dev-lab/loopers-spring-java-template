package com.loopers.domain.ranking;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 주간 랭킹 복합 PK
 * - product_id + year_week 조합으로 유일성 보장
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class WeeklyRankId implements Serializable {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "base_year_week", nullable = false, length = 8)
    private String yearWeek;  // e.g., "2024-W52"

    private WeeklyRankId(Long productId, String yearWeek) {
        this.productId = productId;
        this.yearWeek = yearWeek;
    }

    public static WeeklyRankId of(Long productId, String yearWeek) {
        return new WeeklyRankId(productId, yearWeek);
    }
}
