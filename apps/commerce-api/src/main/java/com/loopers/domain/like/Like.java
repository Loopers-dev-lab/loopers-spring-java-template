package com.loopers.domain.like;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "product_like", uniqueConstraints = @UniqueConstraint(
        columnNames = {"user_id", "product_id"})
)
public class Like extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Builder
    public Like(String userId, Long productId) {
        this.userId = userId;
        this.productId = productId;
    }
}
