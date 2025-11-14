package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductCondition;
import com.loopers.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;
    private final ProductQueryRepository productQueryRepository;

    @Override
    public Optional<Product> save(Product product) {
        Product savedProduct = productJpaRepository.save(product);
        return Optional.of(savedProduct);
    }

    @Override
    public Optional<Product> findById(Long productId) {
        return productJpaRepository.findById(productId);
    }

    @Override
    public Optional<Product> findByIdWithBrand(Long productId) {
        return productQueryRepository.findByIdWithBrand(productId);
    }

    @Override
    public Page<Product> findProducts(ProductCondition condition, Pageable pageable) {
        return productQueryRepository.findProducts(condition, pageable);
    }

    @Override
    public void incrementLikeCount(Long productId) {
        productQueryRepository.incrementLikeCount(productId);
    }

    @Override
    public void decrementLikeCount(Long productId) {
        productQueryRepository.decrementLikeCount(productId);
    }
}
