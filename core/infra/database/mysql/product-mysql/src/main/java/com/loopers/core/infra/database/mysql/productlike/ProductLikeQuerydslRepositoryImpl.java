package com.loopers.core.infra.database.mysql.productlike;


import com.loopers.core.domain.common.type.OrderSort;
import com.loopers.core.infra.database.mysql.productlike.dto.LikeProductListProjection;
import com.loopers.core.infra.database.mysql.productlike.dto.QLikeProductListProjection;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.loopers.core.infra.database.mysql.product.entity.QProductEntity.productEntity;
import static com.loopers.core.infra.database.mysql.productlike.entity.QProductLikeEntity.productLikeEntity;

@Component
@RequiredArgsConstructor
public class ProductLikeQuerydslRepositoryImpl implements ProductLikeQuerydslRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<LikeProductListProjection> findLikeProductListWithCondition(
            Long userId,
            Long brandId,
            String createdAtSort,
            String priceSort,
            String likeCountSort,
            Pageable pageable
    ) {
        List<LikeProductListProjection> content = queryFactory
                .select(new QLikeProductListProjection(
                        productEntity.id,
                        productEntity.brandId,
                        productEntity.name,
                        productEntity.price,
                        productEntity.stock,
                        productEntity.likeCount,
                        productEntity.createdAt,
                        productEntity.updatedAt
                ))
                .from(productLikeEntity)
                .join(productEntity).on(productEntity.id.eq(productLikeEntity.productId))
                .where(
                        eqProductBrandId(brandId),
                        eqProductLikeUserId(userId),
                        productEntity.deletedAt.isNull()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(
                        orderByCreatedAt(createdAtSort),
                        orderByPrice(priceSort),
                        orderByLikeCount(likeCountSort)
                ).fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(productEntity.count())
                .from(productEntity)
                .join(productEntity).on(productEntity.id.eq(productLikeEntity.productId))
                .where(
                        eqProductBrandId(brandId),
                        eqProductLikeUserId(userId),
                        productEntity.deletedAt.isNull()
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression eqProductLikeUserId(Long userId) {
        return Optional.ofNullable(userId)
                .map(productLikeEntity.userId::eq)
                .orElse(null);
    }

    private BooleanExpression eqProductBrandId(Long brandId) {
        return Optional.ofNullable(brandId)
                .map(productEntity.brandId::eq)
                .orElse(null);
    }

    private OrderSpecifier<?> orderByCreatedAt(String sort) {
        OrderSort createdAt = OrderSort.from(sort);
        if (createdAt == OrderSort.ASC) return productEntity.createdAt.asc();
        if (createdAt == OrderSort.DESC) return productEntity.createdAt.desc();

        return null;
    }

    private OrderSpecifier<?> orderByPrice(String sort) {
        OrderSort price = OrderSort.from(sort);
        if (price == OrderSort.ASC) return productEntity.price.asc();
        if (price == OrderSort.DESC) return productEntity.price.desc();

        return null;
    }

    private OrderSpecifier<?> orderByLikeCount(String sort) {
        OrderSort likeCount = OrderSort.from(sort);
        if (likeCount == OrderSort.ASC) return productEntity.likeCount.asc();
        if (likeCount == OrderSort.DESC) return productEntity.likeCount.desc();

        return null;
    }
}
