package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.view.ProductCondition;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.view.ProductView;
import com.loopers.domain.product.view.ProductViewRepository;
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
    private final ProductViewRepository productViewRepository;

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
    public Page<ProductView> findProductViews(ProductCondition condition, Pageable pageable) {
        return productQueryRepository.findProductViews(condition, pageable);
    }

    @Override
    public Optional<ProductView> findProductViewById(Long productId) {
        return productViewRepository.findById(productId);
    }
}
