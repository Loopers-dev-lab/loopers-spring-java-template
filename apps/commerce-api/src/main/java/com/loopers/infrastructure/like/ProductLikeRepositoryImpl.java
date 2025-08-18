package com.loopers.infrastructure.like;

import com.loopers.domain.like.product.ProductLikeModel;
import com.loopers.domain.like.product.ProductLikeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ProductLikeRepositoryImpl implements ProductLikeRepository {
    private final ProductLikeJpaRepository productLikeJpaRepository;

    public ProductLikeRepositoryImpl(ProductLikeJpaRepository productLikeJpaRepository) {
        this.productLikeJpaRepository = productLikeJpaRepository;
    }

    @Override
    public Optional<ProductLikeModel> findByUserIdAndProductId(Long userId, Long productId) {
        return productLikeJpaRepository.findByUserIdAndProductId(userId, productId);
    }

    @Override
    public ProductLikeModel save(ProductLikeModel productLikeModel) {
        return productLikeJpaRepository.save(productLikeModel);
    }

    @Override
    public void delete(ProductLikeModel productLikeModel) {
        productLikeJpaRepository.delete(productLikeModel);
    }

    @Override
    public boolean existsByUserIdAndProductId(Long userId, Long productId) {
        return productLikeJpaRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Override
    public void deleteAll() {
        productLikeJpaRepository.deleteAll();
    }

    @Override
    public Page<ProductLikeModel> findByUserIdOrderByLikedAtDesc(Long userId, Pageable pageable) {
        return productLikeJpaRepository.findByUserIdOrderByLikedAtDesc(userId, pageable);
    }

    @Override
    public long count() {
        return productLikeJpaRepository.count();
    }
}
