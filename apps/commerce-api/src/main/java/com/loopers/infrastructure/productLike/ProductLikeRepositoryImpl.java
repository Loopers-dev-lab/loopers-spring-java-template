package com.loopers.infrastructure.productLike;

import com.loopers.domain.like.ProductLikeModel;
import com.loopers.domain.like.ProductLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class ProductLikeRepositoryImpl implements ProductLikeRepository {

    private final ProductLikeJpaRepository productLikeJpaRepository;

    @Override
    public boolean existsByUserIdAndProductId(Long userId, Long productId) {
        return productLikeJpaRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Override
    public Optional<ProductLikeModel> findByUserIdAndProductId(Long userId, Long productId) {
        return productLikeJpaRepository.findByUserIdAndProductId(userId, productId);
    }

    @Override
    public ProductLikeModel save(ProductLikeModel like) {
        return productLikeJpaRepository.save(like);
    }

    @Override
    public void delete(Long userPkId, Long productId) {
        productLikeJpaRepository.deleteByUserIdAndProductId(userPkId, productId);
    }

    @Override
    public int insertIgnore(Long userPkId, Long productId) {
        return productLikeJpaRepository.insertIgnore(userPkId, productId);
    }

    @Override
    public int countByProductId(Long productId) {
        return productLikeJpaRepository.countByProductId(productId);
    }
}
