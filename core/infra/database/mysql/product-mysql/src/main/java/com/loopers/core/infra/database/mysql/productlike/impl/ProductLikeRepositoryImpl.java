package com.loopers.core.infra.database.mysql.productlike.impl;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.common.type.OrderSort;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.productlike.LikeProductListView;
import com.loopers.core.domain.productlike.ProductLike;
import com.loopers.core.domain.productlike.repository.ProductLikeRepository;
import com.loopers.core.domain.user.vo.UserId;
import com.loopers.core.infra.database.mysql.productlike.ProductLikeJpaRepository;
import com.loopers.core.infra.database.mysql.productlike.dto.LikeProductListProjection;
import com.loopers.core.infra.database.mysql.productlike.entity.ProductLikeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductLikeRepositoryImpl implements ProductLikeRepository {

    private final ProductLikeJpaRepository repository;

    @Override
    @Transactional
    public void deleteByUserIdAndProductId(UserId userId, ProductId productId) {
        repository.deleteByUserIdAndProductId(
                Long.parseLong(userId.value()),
                Long.parseLong(productId.value())
        );
    }

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

    @Override
    public Optional<ProductLike> findByUserIdAndProductIdWithLock(UserId userId, ProductId productId) {
        return repository.findByUserIdAndProductIdWithLock(
                Long.parseLong(userId.value()),
                Long.parseLong(productId.value())
        ).map(ProductLikeEntity::to);
    }

    @Override
    public LikeProductListView findLikeProductsListWithCondition(
            UserId userId,
            BrandId brandId,
            OrderSort createdAtSort,
            OrderSort priceSort,
            OrderSort likeCountSort,
            int pageNo,
            int pageSize
    ) {
        Page<LikeProductListProjection> page = repository.findLikeProductListWithCondition(
                Optional.ofNullable(userId.value())
                        .map(Long::parseLong)
                        .orElse(null),
                Optional.ofNullable(brandId.value())
                        .map(Long::parseLong)
                        .orElse(null),
                createdAtSort,
                priceSort,
                likeCountSort,
                PageRequest.of(pageNo, pageSize)
        );

        return new LikeProductListView(
                page.getContent().stream()
                        .map(LikeProductListProjection::to)
                        .toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
