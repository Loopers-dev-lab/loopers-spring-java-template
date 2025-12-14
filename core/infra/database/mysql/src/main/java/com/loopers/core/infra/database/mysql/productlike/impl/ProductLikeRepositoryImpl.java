package com.loopers.core.infra.database.mysql.productlike.impl;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.common.type.OrderSort;
import com.loopers.core.domain.productlike.LikeProductListView;
import com.loopers.core.domain.productlike.ProductLike;
import com.loopers.core.domain.productlike.ProductLikeCache;
import com.loopers.core.domain.productlike.repository.ProductLikeRepository;
import com.loopers.core.domain.user.vo.UserId;
import com.loopers.core.infra.database.mysql.productlike.ProductLikeJpaRepository;
import com.loopers.core.infra.database.mysql.productlike.dto.LikeProductListProjection;
import com.loopers.core.infra.database.mysql.productlike.entity.ProductLikeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    @Override
    public void bulkSaveOrUpdate(List<ProductLike> productLikes) {
        repository.bulkSaveOrUpdate(productLikes.stream().map(ProductLikeEntity::from).toList());
    }

    @Override
    public void bulkDelete(List<ProductLikeCache> productLikeCaches) {
        repository.bulkDelete(productLikeCaches);
    }

    @Override
    public List<ProductLike> findAll() {
        return repository.findAll().stream()
                .map(ProductLikeEntity::to)
                .toList();
    }
}
