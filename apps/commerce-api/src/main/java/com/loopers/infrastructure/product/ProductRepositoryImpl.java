package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Product registerProduct(Product product) {
        return productJpaRepository.save(product);
    }

    @Override
    public boolean existsProductCode(String productCode) {
        return productJpaRepository.existsByProductCode(productCode);
    }
}
