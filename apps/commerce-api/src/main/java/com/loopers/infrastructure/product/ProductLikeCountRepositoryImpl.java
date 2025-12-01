package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductLikeCount;
import com.loopers.domain.product.ProductLikeCountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class ProductLikeCountRepositoryImpl implements ProductLikeCountRepository {
    private final ProductLikeCountJpaRepository productLikeCountJpaRepository;

    @Override
    public Optional<ProductLikeCount> findById(Long productId) {
        return productLikeCountJpaRepository.findById(productId);
    }

    @Override
    public ProductLikeCount save(ProductLikeCount productLikeCount) {
        return productLikeCountJpaRepository.save(productLikeCount);
    }

    public int upsertLikeCount(Long id) {
        return productLikeCountJpaRepository.upsertLikeCount(id);
    }

    public int decreaseLikeCount(Long id) {
        return productLikeCountJpaRepository.decreaseLikeCountById(id);
    }

    @Override
    public Page<ProductLikeCount> findPageOrderByLikeCountDesc(Long brandId, Pageable pageRequest) {
        return productLikeCountJpaRepository.findPageOrderByLikeCountDesc(brandId, pageRequest);
    }
}
