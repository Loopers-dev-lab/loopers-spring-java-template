package com.loopers.infrastructure.product;

import com.loopers.domain.product.view.ProductCondition;
import com.loopers.domain.product.view.ProductView;
import com.loopers.domain.product.view.QProductView;
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

@RequiredArgsConstructor
@Component
public class ProductQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final QProductView productView = QProductView.productView;

    /**
     * ProductView 기반 조회
     * Materialized View를 사용하여 빠른 조회 성능 제공
     */
    public Page<ProductView> findProductViews(ProductCondition condition, Pageable pageable) {
        // 1. where 조건 생성
        BooleanBuilder whereCondition = new BooleanBuilder();
        
        if (condition.brandId() != null) {
            whereCondition.and(productView.brandId.eq(condition.brandId()));
        }
        
        // 2. 정렬 조건
        final Map<String, OrderSpecifier<?>> ORDER_BY_MAP = Map.of(
            "price_asc", productView.price.asc(),
            "likes_desc", productView.likeCount.desc(),
            "latest", productView.createdAt.desc()
        );
        
        OrderSpecifier<?> orderSpecifier = ORDER_BY_MAP.getOrDefault(
            condition.sort(),
            productView.createdAt.desc() // 기본값: 최신순
        );
        
        // 3. 데이터 조회 (ProductView 사용)
        List<ProductView> products = queryFactory
                .selectFrom(productView)
                .where(whereCondition)
                .orderBy(orderSpecifier)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        
        // 4. 전체 개수 조회
        Long total = queryFactory
                .select(productView.count())
                .from(productView)
                .where(whereCondition)
                .fetchOne();
        
        return new PageImpl<>(products, pageable, total != null ? total : 0L);
    }

    /**
     * ProductView 전체 개수만 조회 (효율적인 count 쿼리)
     */
    public long countProductViews(ProductCondition condition) {
        BooleanBuilder whereCondition = new BooleanBuilder();
        
        if (condition.brandId() != null) {
            whereCondition.and(productView.brandId.eq(condition.brandId()));
        }
        
        Long total = queryFactory
                .select(productView.count())
                .from(productView)
                .where(whereCondition)
                .fetchOne();
        
        return total != null ? total : 0L;
    }
}

