package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.interfaces.api.product.ProductV1Dto;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.loopers.domain.like.QLike.like;
import static com.loopers.domain.product.QProduct.product;

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

    @Override
    public List<Product> searchProductsByCondition(ProductV1Dto.SearchProductRequest request) {
        System.out.println("sortBy = " + request.sortCondition().sortBy());
        boolean isAsc = request.sortCondition().order().equals("asc");

        JPAQuery<Product> query = queryFactory
                .selectFrom(product)
                .leftJoin(like)
                .on(product.id.eq(like.productId))
                .groupBy(product.id);


        switch (request.sortCondition().sortBy()) {
            case "price" -> query.orderBy(isAsc ? product.price.asc() : product.price.desc());
            case "createdAt" -> query.orderBy(isAsc ? product.createdAt.asc() : product.createdAt.desc());
            case "likeCount" -> query.orderBy(isAsc ? like.count().asc() : like.count().desc());
        }

        return query.fetch();
    }
}
