package com.loopers.domain.like;

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
@Table(name = "brand_like", 
       uniqueConstraints = @UniqueConstraint(
           name = "uk_brand_like_user_brand", 
           columnNames = {"user_id", "brand_id"}
       ))
@Getter
public class BrandLikeModel extends BaseEntity {
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "brand_id", nullable = false)
    private Long brandId;
    
    @Column(name = "liked_at", nullable = false)
    private LocalDateTime likedAt;
    
    protected BrandLikeModel() {}
    
    private BrandLikeModel(Long userId, Long brandId) {
        this.userId = userId;
        this.brandId = brandId;
        this.likedAt = LocalDateTime.now();
    }
    
    public static BrandLikeModel create(Long userId, Long brandId) {
        validateCreateParameters(userId, brandId);
        return new BrandLikeModel(userId, brandId);
    }
    
    public void remove() {
        // Soft delete가 필요한 경우 구현
        // 현재는 entity 자체를 삭제하는 것으로 가정
    }
    
    public boolean belongsToUser(Long userId) {
        return this.userId.equals(userId);
    }
    
    public boolean isForBrand(Long brandId) {
        return this.brandId.equals(brandId);
    }
    
    private static void validateCreateParameters(Long userId, Long brandId) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
        }
        if (brandId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 ID는 필수입니다.");
        }
    }
}
