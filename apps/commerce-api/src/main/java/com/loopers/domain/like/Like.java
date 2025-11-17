package com.loopers.domain.like;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Entity
@Table(
        name = "likes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_product", columnNames = {"user_id", "product_id"})
        }
)
@Getter
public class Like extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    protected Like() {
    }

    private Like(Long userId, Long productId) {
        this.userId = userId;
        this.productId = productId;
    }

    public static Like createLike(Long userId, Long productId) {
        return new Like(userId, productId);
    }
}
