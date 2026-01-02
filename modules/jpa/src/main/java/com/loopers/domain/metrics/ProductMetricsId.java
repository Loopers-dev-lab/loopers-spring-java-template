package com.loopers.domain.metrics;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 상품 메트릭 복합 PK
 * <p>
 * product_id + metric_date 조합으로 일간 집계를 구분합니다.
 * Hibernate 6.x 권장 방식인 @Embeddable + @EmbeddedId 패턴을 사용합니다.
 *
 * @author hyunjikoh
 * @since 2025. 12. 31.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class ProductMetricsId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    private ProductMetricsId(Long productId, LocalDate metricDate) {
        Objects.requireNonNull(productId, "상품 ID는 필수입니다.");
        Objects.requireNonNull(metricDate, "메트릭 날짜는 필수입니다.");
        this.productId = productId;
        this.metricDate = metricDate;
    }

    /**
     * 복합키 생성 팩토리 메서드
     *
     * @param productId  상품 ID
     * @param metricDate 메트릭 날짜
     * @return ProductMetricsId 인스턴스
     */
    public static ProductMetricsId of(Long productId, LocalDate metricDate) {
        return new ProductMetricsId(productId, metricDate);
    }
}
