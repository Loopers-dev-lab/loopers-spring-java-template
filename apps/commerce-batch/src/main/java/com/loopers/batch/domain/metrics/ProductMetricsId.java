package com.loopers.batch.domain.metrics;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMetricsId implements Serializable {

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "metrics_date")
    private LocalDate metricsDate;
}
