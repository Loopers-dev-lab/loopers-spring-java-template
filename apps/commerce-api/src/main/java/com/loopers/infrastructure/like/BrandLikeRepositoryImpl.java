package com.loopers.infrastructure.like;

import com.loopers.domain.like.BrandLikeModel;
import com.loopers.domain.like.BrandLikeRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class BrandLikeRepositoryImpl implements BrandLikeRepository {
    private final BrandLikeJpaRepository brandLikeJpaRepository;

    public BrandLikeRepositoryImpl(BrandLikeJpaRepository brandLikeJpaRepository) {
        this.brandLikeJpaRepository = brandLikeJpaRepository;
    }

    @Override
    public Optional<BrandLikeModel> findByUserIdAndBrandId(Long userId, Long brandId) {
        return brandLikeJpaRepository.findByUserIdAndBrandId(userId, brandId);
    }

    @Override
    public BrandLikeModel save(BrandLikeModel brandLikeModel) {
        return brandLikeJpaRepository.save(brandLikeModel);
    }

    @Override
    public void delete(BrandLikeModel brandLikeModel) {
        brandLikeJpaRepository.delete(brandLikeModel);
    }

    @Override
    public boolean existsByUserIdAndBrandId(Long userId, Long brandId) {
        return brandLikeJpaRepository.existsByUserIdAndBrandId(userId, brandId);
    }

    @Override
    public void deleteAll() {
        brandLikeJpaRepository.deleteAll();
    }

    @Override
    public long count() {
        return brandLikeJpaRepository.count();
    }
}
