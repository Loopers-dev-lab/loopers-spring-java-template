package com.loopers.infrastructure.like;

import com.loopers.domain.like.brand.BrandLikeModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BrandLikeJpaRepository extends JpaRepository<BrandLikeModel, Long> {
    Optional<BrandLikeModel> findByUserIdAndBrandId(Long userId, Long brandId);

    boolean existsByUserIdAndBrandId(Long userId, Long brandId);
}
