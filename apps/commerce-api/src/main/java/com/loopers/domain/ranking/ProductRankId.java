package com.loopers.domain.ranking;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor
public class ProductRankId implements Serializable {

    @Column(name = "period_key", nullable = false)
    private String periodKey;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    private ProductRankId(String periodKey, Long productId) {
        this.periodKey = periodKey;
        this.productId = productId;
    }

    public static ProductRankId of(String periodKey, Long productId) {
        return new ProductRankId(periodKey, productId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductRankId that)) {
            return false;
        }
        return Objects.equals(periodKey, that.periodKey)
                && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periodKey, productId);
    }
}
