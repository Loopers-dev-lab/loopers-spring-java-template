package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSortType;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ProductRepositoryImpl implements ProductRepository {
    private final ProductJpaRepository productJpaRepository;
    private final ProductQueryRepository productQueryRepository;

    @Override
    public Optional<Product> findByIdWithLock(final Long productId) {
        return productJpaRepository.findByIdWithLock(productId);
    }

    @Override
    public List<Product> getProductList(final ProductSortType sortType) {
        return productQueryRepository.getProductList(sortType);
    }

    @Override
    public List<Product> findAllByIdInWithLock(final List<Long> productIdList) {
        return productJpaRepository.findAllByIdInWithLock(productIdList);
    }

    @Override
    public Optional<Product> findById(final Long productId) {
        return productJpaRepository.findById(productId);
    }
}
