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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

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
            OrderSort createdAtSort,
            OrderSort priceSort,
            OrderSort likeCountSort,
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
                        productEqBrandId(brandId),
                        productLikeEqUserId(userId),
                        productEntity.deletedAt.isNull()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(
                        Stream.of(
                                        orderByCreatedAt(createdAtSort),
                                        orderByPrice(priceSort),
                                        orderByLikeCount(likeCountSort)
                                ).filter(Objects::nonNull)
                                .toArray(OrderSpecifier[]::new)
                ).fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(productLikeEntity.count())
                .from(productLikeEntity)
                .join(productEntity).on(productEntity.id.eq(productLikeEntity.productId))
                .where(
                        productEqBrandId(brandId),
                        productLikeEqUserId(userId),
                        productEntity.deletedAt.isNull()
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression productLikeEqUserId(Long userId) {
        return Optional.ofNullable(userId)
                .map(productLikeEntity.userId::eq)
                .orElse(null);
    }

    private BooleanExpression productEqBrandId(Long brandId) {
        return Optional.ofNullable(brandId)
                .map(productEntity.brandId::eq)
                .orElse(null);
    }

    private OrderSpecifier<?> orderByCreatedAt(OrderSort sort) {
        if (sort == OrderSort.ASC) return productEntity.createdAt.asc();
        if (sort == OrderSort.DESC) return productEntity.createdAt.desc();

        return null;
    }

    private OrderSpecifier<?> orderByPrice(OrderSort sort) {
        if (sort == OrderSort.ASC) return productEntity.price.asc();
        if (sort == OrderSort.DESC) return productEntity.price.desc();

        return null;
    }

    private OrderSpecifier<?> orderByLikeCount(OrderSort sort) {
        if (sort == OrderSort.ASC) return productEntity.likeCount.asc();
        if (sort == OrderSort.DESC) return productEntity.likeCount.desc();

        return null;
    }
}
