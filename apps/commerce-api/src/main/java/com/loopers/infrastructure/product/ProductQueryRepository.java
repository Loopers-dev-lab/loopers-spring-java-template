package com.loopers.infrastructure.product;

import static com.loopers.domain.product.QProduct.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductSortType;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ProductQueryRepository {
    private final JPAQueryFactory queryFactory;

    public List<Product> getProductList(final Long brandId, final ProductSortType sortType, final Pageable pageable) {
        return queryFactory
                .selectFrom(product)
                .where(brandIdEquals(brandId))
                .orderBy(sortCondition(sortType))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    private BooleanExpression brandIdEquals(Long brandId) {
        if (brandId == null) {
            return null;
        }
        return product.brandId.eq(brandId);
    }

    private OrderSpecifier<?> sortCondition(final ProductSortType sortType) {
        if (sortType == ProductSortType.LATEST) {
            return product.createdAt.desc();
        }
        if (sortType == ProductSortType.PRICE_ASC) {
            return product.price.asc();
        }
        if (sortType == ProductSortType.LIKES_DESC) {
            return product.likeCount.value.desc();
        }

        return null;
    }
}
