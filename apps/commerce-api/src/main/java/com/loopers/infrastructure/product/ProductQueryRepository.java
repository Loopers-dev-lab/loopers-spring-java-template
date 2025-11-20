package com.loopers.infrastructure.product;

import static com.loopers.domain.product.QProduct.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductSortType;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ProductQueryRepository {
    private final JPAQueryFactory queryFactory;

    public List<Product> getProductList(ProductSortType sortType) {
        return queryFactory
                .selectFrom(product)
                .orderBy(sortCondition(sortType))
                .fetch();
    }

    private OrderSpecifier<?> sortCondition(ProductSortType sortType) {
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
