package com.loopers.core.infra.database.mysql.productlike.impl;

import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.productlike.ProductLike;
import com.loopers.core.domain.productlike.repository.ProductLikeRepository;
import com.loopers.core.domain.user.vo.UserId;
import com.loopers.core.infra.database.mysql.productlike.ProductLikeJpaRepository;
import com.loopers.core.infra.database.mysql.productlike.entity.ProductLikeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductLikeRepositoryImpl implements ProductLikeRepository {

    private final ProductLikeJpaRepository repository;

    @Override
    public ProductLike save(ProductLike productLike) {
        return repository.save(ProductLikeEntity.from(productLike)).to();
    }

    @Override
    public Optional<ProductLike> findByUserIdAndProductId(UserId userId, ProductId productId) {
        return repository.findByUserIdAndProductId(
                Long.parseLong(userId.value()),
                Long.parseLong(productId.value())
        ).map(ProductLikeEntity::to);
    }
}
