package com.loopers.domain.product;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class ProductService {
    private final ProductRepository productRepository;

    public Optional<Product> getProductById(Long productId) {
        return productRepository.findById(productId);
    }

    public List<Product> getProductsByIds(Collection<Long> productIds) {
        return productRepository.findAllByIdIn(productIds);
    }

    public Map<Long, Product> getProductMapByIds(Collection<Long> productIds) {
        return productRepository.findAllByIdIn(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
    }

    public Page<Product> getProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public Page<Product> getProductsByBrandIds(List<Long> brandIds, Pageable pageable) {
        if (brandIds == null || brandIds.isEmpty()) {
            return productRepository.findAll(pageable);
        }
        return productRepository.findAllByBrandIdIn(brandIds, pageable);
    }

    @Transactional
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public List<Product> saveAll(Collection<Product> products) {
        return productRepository.saveAll(products);
    }
}
