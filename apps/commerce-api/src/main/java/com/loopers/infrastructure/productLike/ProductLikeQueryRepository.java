package com.loopers.infrastructure.productLike;

import com.loopers.domain.like.ProductLikeSummaryVO;
import com.loopers.domain.like.QProductLikeModel;
import com.loopers.domain.product.ProductSortType;
import com.loopers.domain.product.QProductModel;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductLikeQueryRepository {
    private final JPAQueryFactory queryFactory;

    public Page<ProductLikeSummaryVO> findProductLikes(ProductSortType sortType,
                                                       Pageable pageable) {

        QProductModel product = QProductModel.productModel;
        QProductLikeModel productLike = QProductLikeModel.productLikeModel;

        JPAQuery<ProductLikeSummaryVO> query = queryFactory
                .select(Projections.constructor(
                        ProductLikeSummaryVO.class,
                        product.id,
                        product.name,
                        product.price,
                        product.status,
                        productLike.id.count()   // likeCount 집계
                ))
                .from(product)
                .leftJoin(productLike).on(productLike.product.eq(product))
                .groupBy(
                        product.id,
                        product.name,
                        product.brand.id,
                        product.brand.name,
                        product.price,
                        product.status
                );

        applySort(sortType, query, product, productLike);

        List<ProductLikeSummaryVO> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, content.size());
    }

    public Optional<ProductLikeSummaryVO> findProductDetail(Long productId) {
        QProductModel product = QProductModel.productModel;
        QProductLikeModel productLike = QProductLikeModel.productLikeModel;

        ProductLikeSummaryVO result = queryFactory
                .select(Projections.constructor(
                        ProductLikeSummaryVO.class,
                        product.id,
                        product.name,
                        product.brand.id,
                        product.brand.name,
                        product.price,
                        product.status,
                        productLike.id.count()
                ))
                .from(product)
                .leftJoin(productLike).on(productLike.product.eq(product))
                .where(
                        product.id.eq(productId)
                )
                .groupBy(
                        product.id,
                        product.name,
                        product.brand.id,
                        product.brand.name,
                        product.price,
                        product.status
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    private void applySort(ProductSortType sortType, JPAQuery<?> query, QProductModel product, QProductLikeModel productLike) {
        if (sortType == null || sortType == ProductSortType.DEFAULT) {
            query.orderBy(productLike.id.count().desc(), productLike.id.asc());
            return;
        }

        switch(sortType) {
            case LIKE_ASC -> query.orderBy(productLike.id.count().asc(), productLike.id.asc());
            case LIKE_DESC -> query.orderBy(productLike.id.count().desc(), productLike.id.asc());
            default -> query.orderBy(productLike.id.count().desc(), productLike.id.asc());
        }
    }

}
