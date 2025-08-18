package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.QProductModel;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProductRepositoryImpl implements ProductRepository {
    private final JPAQueryFactory queryFactory;
    private final ProductJpaRepository productJpaRepository;

    public ProductRepositoryImpl(JPAQueryFactory queryFactory, ProductJpaRepository productJpaRepository) {
        this.queryFactory = queryFactory;
        this.productJpaRepository = productJpaRepository;
    }
    @Override
    public Page<ProductModel> search(Long brandId, String sort, int page, int size) {
        QProductModel product = QProductModel.productModel;

        BooleanBuilder builder = new BooleanBuilder();
        if(brandId != null){
            builder.and(Expressions.numberPath(Long.class, product.brandId, "brandId").eq(brandId));
        }
        // 정렬 기준
        OrderSpecifier<?> orderSpecifier = getOrderSpecifier(product, sort);
        // 페이징
        Pageable pageable = PageRequest.of(page, size);

        List<ProductModel> content = queryFactory
                .selectFrom(product)
                .where(builder)
                .orderBy(orderSpecifier)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = queryFactory
                .select(product.count())
                .from(product)
                .where(builder)
                .fetchOne();
        
        long total = totalCount != null ? totalCount : 0L;

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public ProductModel save(ProductModel productModel) {
        return productJpaRepository.save(productModel);
    }

    @Override
    public void deleteAll() {
        productJpaRepository.deleteAll();
    }

    @Override
    public Optional<ProductModel> findById(Long productId) {
        if(productId == null) return Optional.empty();
        return productJpaRepository.findById(productId);
    }
    
    @Override
    public Optional<ProductModel> findByIdForUpdate(Long id) {
        if(id == null) return Optional.empty();
        return productJpaRepository.findByIdForUpdate(id);
    }

    @Override
    public List<ProductModel> findByIdIn(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return productJpaRepository.findByIdIn(productIds);
    }

    private OrderSpecifier<?> getOrderSpecifier(QProductModel product, String sort) {
        if (sort == null || sort.equals("latest")) {
            return product.createdAt.desc();
        }
        return switch (sort) {
            case "price_asc" -> product.price.price.asc();
            case "likes_desc" -> product.LikeCount.productLikeCount.desc();
            default -> product.createdAt.desc();
        };
    }
}
