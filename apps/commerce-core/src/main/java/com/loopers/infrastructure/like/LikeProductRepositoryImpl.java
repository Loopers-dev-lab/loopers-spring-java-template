package com.loopers.infrastructure.like;

import com.loopers.domain.like.product.LikeProduct;
import com.loopers.domain.like.product.LikeProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class LikeProductRepositoryImpl implements LikeProductRepository {
    private final LikeProductJpaRepository likeProductJpaRepository;

    @Override
    public Optional<LikeProduct> findByUserIdAndProductId(Long userId, Long productId) {
        return likeProductJpaRepository.findByUserIdAndProductId(userId, productId);
    }

    @Override
    public void save(LikeProduct likeProduct) {
        likeProductJpaRepository.save(likeProduct);
    }

    @Override
    public Page<LikeProduct> getLikeProductsByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable) {
        return likeProductJpaRepository.getLikeProductsByUserIdAndDeletedAtIsNull(userId, pageable);
    }

}
