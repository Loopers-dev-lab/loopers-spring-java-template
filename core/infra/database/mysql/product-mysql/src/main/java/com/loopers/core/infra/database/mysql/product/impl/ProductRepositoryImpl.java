package com.loopers.core.infra.database.mysql.product.impl;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.common.type.OrderSort;
import com.loopers.core.domain.common.vo.PageNo;
import com.loopers.core.domain.common.vo.PageSize;
import com.loopers.core.domain.error.NotFoundException;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.ProductListView;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.infra.database.mysql.product.ProductJpaRepository;
import com.loopers.core.infra.database.mysql.product.dto.ProductListProjection;
import com.loopers.core.infra.database.mysql.product.entity.ProductEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository repository;

    @Override
    public Product getById(ProductId productId) {
        return repository.findById(
                        Objects.requireNonNull(Optional.ofNullable(productId.value())
                                .map(Long::parseLong)
                                .orElse(null))
                ).map(ProductEntity::to)
                .orElseThrow(() -> NotFoundException.withName("상품"));
    }

    @Override
    public ProductListView findListWithCondition(
            BrandId brandId,
            OrderSort createdAtSort,
            OrderSort priceSort,
            OrderSort likeCountSort,
            PageNo pageNo,
            PageSize pageSize
    ) {
        Page<ProductListProjection> page = repository.findListWithCondition(
                Optional.ofNullable(brandId.value())
                        .map(Long::parseLong)
                        .orElse(null),
                createdAtSort.name(),
                priceSort.name(),
                likeCountSort.name(),
                PageRequest.of(pageNo.value(), pageSize.value())
        );

        return new ProductListView(
                page.getContent().stream()
                        .map(ProductListProjection::to)
                        .toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }

    @Override
    public Product save(Product product) {
        return repository.save(ProductEntity.from(product)).to();
    }
}
