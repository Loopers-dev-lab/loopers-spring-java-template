package com.loopers.domain.like;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

/**
 * packageName : com.loopers.domain.like
 * fileName     : Like
 * author      : byeonsungmun
 * date        : 2025. 11. 11.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 11.     byeonsungmun       최초 생성
 */
@Entity
@Table(
        name = "product_like",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"ref_user_id", "ref_product_id"})})
@Getter
public class Like extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ref_user_id", nullable = false)
    private String userId;

    @Column(name = "ref_product_id", nullable = false)
    private Long productId;

    protected Like() {
    }

    private Like(String userId, Long productId) {
        this.userId = requireValidUserId(userId);
        this.productId = requireValidProductId(productId);
    }

    public static Like create(String userId, Long productId) {
        return new Like(userId, productId);
    }

    private String requireValidUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
        }
        return userId;
    }

    private Long requireValidProductId(Long productId) {
        if (productId == null || productId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 필수입니다.");
        }
        return productId;
    }
}
