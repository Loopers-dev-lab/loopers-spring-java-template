package com.loopers.infrastructure.like;

import com.loopers.domain.like.ProductLike;
import com.loopers.domain.like.ProductLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductLikeRepositoryImpl implements ProductLikeRepository {

    private final ProductLikeJpaRepository productLikeJpaRepository;

    @Override
    public Optional<ProductLike> findByUserIdAndProductId(Long userId, Long productId) {
        return productLikeJpaRepository.findByUserIdAndProductId(userId, productId);
    }

    @Override
    public void delete(ProductLike productLike) {
        productLike.delete();
        productLikeJpaRepository.save(productLike);
    }

    @Override
    public ProductLike save(ProductLike productLike) {
        return productLikeJpaRepository.save(productLike);
    }

    @Override
    public List<ProductLike> findByUserId(Long userId) {
        return productLikeJpaRepository.findByUserId(userId);
    }

    @Override
    public List<Long> findProductIdsByUserId(Long userId) {
        return productLikeJpaRepository.findProductIdsByUserId(userId);
    }
}
