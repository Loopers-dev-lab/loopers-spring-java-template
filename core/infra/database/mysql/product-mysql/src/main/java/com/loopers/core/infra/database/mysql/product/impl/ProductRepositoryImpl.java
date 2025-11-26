package com.loopers.core.infra.database.mysql.product.impl;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.common.type.OrderSort;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository repository;

    @Override
    public void bulkSaveOrUpdate(List<Product> products) {
        repository.bulkSaveOrUpdate(
                products.stream()
                        .map(ProductEntity::from)
                        .toList()
        );
    }

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
    public Product getByIdWithLock(ProductId productId) {
        return repository.findByIdWithLock(
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
            int pageNo,
            int pageSize
    ) {
        Page<ProductListProjection> page = repository.findListWithCondition(
                Optional.ofNullable(brandId.value())
                        .map(Long::parseLong)
                        .orElse(null),
                createdAtSort,
                priceSort,
                likeCountSort,
                PageRequest.of(pageNo, pageSize)
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

    @Override
    public Optional<Product> findById(ProductId productId) {
        return repository.findById(
                Objects.requireNonNull(Optional.ofNullable(productId.value())
                        .map(Long::parseLong)
                        .orElse(null))
        ).map(ProductEntity::to);
    }

    @Override
    public List<Product> findAllByIdIn(List<ProductId> productIds) {
        int batchSize = 1000;
        List<Product> results = new ArrayList<>();

        for (int i = 0; i < productIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, productIds.size());
            List<ProductId> batch = productIds.subList(i, endIndex);

            List<Long> ids = batch.stream()
                    .map(ProductId::value)
                    .filter(Objects::nonNull)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            results.addAll(
                    repository.findAllById(ids).stream()
                            .map(ProductEntity::to)
                            .toList()
            );
        }

        return results;
    }
}
