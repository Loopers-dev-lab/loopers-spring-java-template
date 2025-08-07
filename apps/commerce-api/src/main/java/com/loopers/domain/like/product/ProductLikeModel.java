package com.loopers.domain.like.product;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_like", 
       uniqueConstraints = @UniqueConstraint(
           name = "uk_product_like_user_product", 
           columnNames = {"user_id", "product_id"}
       ))
@Getter
public class ProductLikeModel extends BaseEntity {
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "liked_at", nullable = false)
    private LocalDateTime likedAt;
    
    protected ProductLikeModel() {}
    
    private ProductLikeModel(Long userId, Long productId) {
        this.userId = userId;
        this.productId = productId;
        this.likedAt = LocalDateTime.now();
    }
    
    public static ProductLikeModel create(Long userId, Long productId) {
        validateCreateParameters(userId, productId);
        return new ProductLikeModel(userId, productId);
    }

    public boolean belongsToUser(Long userId) {
        return this.userId.equals(userId);
    }
    
    public boolean isForProduct(Long productId) {
        return this.productId.equals(productId);
    }
    
    private static void validateCreateParameters(Long userId, Long productId) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
        }
        if (productId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 필수입니다.");
        }
    }
}
