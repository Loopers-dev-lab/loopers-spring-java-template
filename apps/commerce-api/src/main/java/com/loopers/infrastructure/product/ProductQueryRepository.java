package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductCondition;
import com.loopers.domain.product.QProduct;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class ProductQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final QProduct product = QProduct.product;

    public Page<Product> findProducts(ProductCondition condition, Pageable pageable) {
    
    // 1. where 조건 생성 (간결하게)
    BooleanBuilder whereCondition = new BooleanBuilder();
    
    // if (condition.price() != null) {
    //     whereCondition.and(product.price.eq(condition.price()));
    // }
    // if (condition.likeCount() != null) {
    //     whereCondition.and(product.likeCount.goe(condition.likeCount()));
    // }
    // if (condition.createdAt() != null) {
    //     whereCondition.and(product.createdAt.goe(condition.createdAt()));
    // }
    
    // 2. 정렬 조건
    final Map<String, OrderSpecifier<?>> ORDER_BY_MAP = Map.of(
        "latest", product.createdAt.desc(),
        "price_asc", product.price.asc(),
        "likes_desc", product.likeCount.desc()
    );
    
    OrderSpecifier<?> orderSpecifier = ORDER_BY_MAP.getOrDefault(
        condition.sort(),
        ORDER_BY_MAP.get("latest")
    );
    
    // 3. 데이터 조회
    List<Product> products = queryFactory
            .selectFrom(product)
            .where(whereCondition)
            .orderBy(orderSpecifier)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();
    
    // 4. 전체 개수 조회
    Long total = queryFactory
            .select(product.count())
            .from(product)
            .where(whereCondition)
            .fetchOne();
    
    return new PageImpl<>(products, pageable, total != null ? total : 0L);
}

    /**
     * 상품 좋아요 수 증가 (동시성 안전)
     * 원자적으로 like_count를 1 증가시킴
     * 
     * @param productId 상품 ID
     */
    public void incrementLikeCount(Long productId) {
        queryFactory
                .update(product)
                .set(product.likeCount, product.likeCount.add(1L))
                .where(product.id.eq(productId))
                .execute();
    }

    /**
     * 상품 좋아요 수 감소 (동시성 안전)
     * 원자적으로 like_count를 1 감소시킴 (0 미만으로는 감소하지 않음)
     * 
     * @param productId 상품 ID
     */
    public void decrementLikeCount(Long productId) {
        queryFactory
                .update(product)
                .set(product.likeCount, product.likeCount.subtract(1L))
                .where(product.id.eq(productId)
                        .and(product.likeCount.gt(0L)))
                .execute();
    }

    /**
     * 상품 ID로 상품과 브랜드 정보를 함께 조회 (fetch join)
     * 도메인 서비스에서 Product + Brand 조합 로직을 위해 사용
     * 
     * @param productId 상품 ID
     * @return Product (Brand 포함)
     */
    public Optional<Product> findByIdWithBrand(Long productId) {
        Product result = queryFactory
                .selectFrom(product)
                .leftJoin(product.brand).fetchJoin()
                .where(product.id.eq(productId))
                .fetchOne();
        return Optional.ofNullable(result);
    }

}

