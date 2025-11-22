package com.loopers.domain.productlike;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ProductLike Entity
 * User-Product 좋아요 관계를 추적
 * UNIQUE 제약: userId + productId 조합 (중복 좋아요 방지)
 */
@Entity
@Table(
    name = "product_like",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_product_like_user_product",
            columnNames = {"ref_user_id", "ref_product_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductLike extends BaseEntity {

  @Column(name = "ref_user_id", nullable = false)
  private Long userId;

  @Column(name = "ref_product_id", nullable = false)
  private Long productId;

  @Column(name = "liked_at", nullable = false)
  private LocalDateTime likedAt;

  private ProductLike(Long userId, Long productId, LocalDateTime likedAt) {
    validateUserId(userId);
    validateProductId(productId);
    validateLikedAt(likedAt);

    this.userId = userId;
    this.productId = productId;
    this.likedAt = likedAt;
  }

  public static ProductLike of(Long userId, Long productId, LocalDateTime likedAt) {
    return new ProductLike(userId, productId, likedAt);
  }

  private void validateUserId(Long userId) {
    if (userId == null) {
      throw new CoreException(ErrorType.INVALID_PRODUCT_LIKE_USER_EMPTY);
    }
  }

  private void validateProductId(Long productId) {
    if (productId == null) {
      throw new CoreException(ErrorType.INVALID_PRODUCT_LIKE_PRODUCT_EMPTY);
    }
  }

  private void validateLikedAt(LocalDateTime likedAt) {
    if (likedAt == null) {
      throw new CoreException(ErrorType.INVALID_PRODUCT_LIKE_LIKED_AT_EMPTY);
    }
  }
}
