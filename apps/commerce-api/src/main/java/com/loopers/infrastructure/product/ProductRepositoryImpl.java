package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public List<Product> findAll(int page, int size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Product> productPage = productJpaRepository.findAll(pageRequest);

        return productPage.getContent();
    }

    @Override
    public List<Product> findByBrandId(Long brandId, int page, int size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Product> productPage = productJpaRepository.findByBrandId(brandId, pageRequest);

        return productPage.getContent();
    }

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }
}
