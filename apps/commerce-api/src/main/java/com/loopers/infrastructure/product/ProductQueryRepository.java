package com.loopers.infrastructure.product;

import static com.loopers.domain.product.QProductEntity.productEntity;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.dto.ProductSearchFilter;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
@Component
@RequiredArgsConstructor
public class ProductQueryRepository {
    private final JPAQueryFactory queryFactory;

    /**
     * 상품 페이징 조회
     *
     * @param searchFilter
     * @return
     */
    public Page<ProductEntity> getProducts(ProductSearchFilter searchFilter) {
        Pageable pageable = searchFilter.pageable();

        // 공통 where 조건 빌드
        BooleanExpression whereCondition = buildWhereCondition(searchFilter);

        // 정렬 조건 빌드
        List<OrderSpecifier<?>> orderSpecifiers = buildOrderSpecifiers(pageable);

        // 조회용 쿼리
        List<ProductEntity> content = queryFactory
                .selectFrom(productEntity)
                .where(whereCondition)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .fetch();

        // 카운트 쿼리 (동일한 조건 적용)
        Long total = queryFactory
                .select(productEntity.count())
                .from(productEntity)
                .where(whereCondition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * pageable 기반 정렬 조건 빌드
     *
     * @param pageable
     * @return
     */
    private List<OrderSpecifier<?>> buildOrderSpecifiers(Pageable pageable) {
        return pageable.getSort().stream()
                .map(sort -> {
                    String property = sort.getProperty();
                    boolean isAsc = sort.isAscending();
                    return switch (property) {
                        case "createdAt" -> isAsc ? productEntity.createdAt.asc() : productEntity.createdAt.desc();
                        case "id" -> isAsc ? productEntity.id.asc() : productEntity.id.desc();
                        case "likeCount" -> isAsc ? productEntity.likeCount.asc() : productEntity.likeCount.desc();
                        case "price" -> isAsc ? productEntity.price.originPrice.asc() : productEntity.price.originPrice.desc();
                        default ->
                            // 기본 정렬: 생성일시 내림차순
                                productEntity.createdAt.desc();
                    };
                })
                .collect(Collectors.toList());
    }

    /**
     * 검색 필터 기반 where 조건 빌드
     *
     * @param searchFilter
     * @return
     */
    private BooleanExpression buildWhereCondition(ProductSearchFilter searchFilter) {
        return productEntity.deletedAt.isNull()
                .and(brandIdEq(searchFilter.brandId()))
                .and(productNameContains(searchFilter.productName()));
    }

    private BooleanExpression brandIdEq(Long brandId) {
        return brandId != null ? productEntity.brandId.eq(brandId) : null;
    }

    private BooleanExpression productNameContains(String productName) {
        return productName != null ? productEntity.name.contains(productName) : null;
    }
}
