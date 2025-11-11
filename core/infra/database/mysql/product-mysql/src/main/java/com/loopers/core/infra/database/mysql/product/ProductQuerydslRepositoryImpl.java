package com.loopers.core.infra.database.mysql.product;

import com.loopers.core.domain.common.type.OrderSort;
import com.loopers.core.infra.database.mysql.product.dto.ProductListProjection;
import com.loopers.core.infra.database.mysql.product.dto.QProductListProjection;
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

@Component
@RequiredArgsConstructor
public class ProductQuerydslRepositoryImpl implements ProductQuerydslRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ProductListProjection> findListWithCondition(
            Long brandId,
            OrderSort createdAtSort,
            OrderSort priceSort,
            OrderSort likeCountSort,
            Pageable pageable
    ) {
        List<ProductListProjection> content = queryFactory
                .select(new QProductListProjection(
                        productEntity.id,
                        productEntity.brandId,
                        productEntity.name,
                        productEntity.price,
                        productEntity.stock,
                        productEntity.likeCount,
                        productEntity.createdAt,
                        productEntity.updatedAt
                ))
                .where(
                        productEqBrandId(brandId),
                        productEntity.deletedAt.isNull()
                )
                .from(productEntity)
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
                .where(
                        productEqBrandId(brandId),
                        productEntity.deletedAt.isNull()
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
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
