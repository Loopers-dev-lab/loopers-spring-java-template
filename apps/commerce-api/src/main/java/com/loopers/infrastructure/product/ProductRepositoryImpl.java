package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.interfaces.api.product.ProductV1Dto;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ProductRepositoryImpl implements ProductRepository {
    private final ProductJpaRepository productJpaRepository;
    private final JPAQueryFactory queryFactory;

    public ProductRepositoryImpl(ProductJpaRepository productJpaRepository, EntityManager entityManager) {
        this.productJpaRepository = productJpaRepository;
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productJpaRepository.findById(id);
    }

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }

    @Override
    public List<Product> findAll() {
        return productJpaRepository.findAll();
    }

}
