package com.loopers.domain.like;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "product_like")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Like extends BaseEntity {
    @Column(name = "ref_user_id", nullable = false)
    private Long userId;

    @Column(name = "ref_product_id", nullable = false)
    private Long productId;

    public Like(Long userId, Long productId) {
        this.userId = userId;
        this.productId = productId;
    }

    public boolean isAlreadyLike(Like like) {
        if (like != null) {
            return true;
        }
        return false;
    }


}
