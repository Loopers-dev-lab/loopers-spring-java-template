package com.loopers.domain.like.fixture;

import com.loopers.domain.like.product.ProductLikeModel;

public class ProductLikeFixture {

    public static final Long DEFAULT_USER_ID = 1L;
    public static final Long DEFAULT_PRODUCT_ID = 1L;

    public static ProductLikeModel createProductLikeModel() {
        return ProductLikeModel.create(DEFAULT_USER_ID, DEFAULT_PRODUCT_ID);
    }

    public static ProductLikeModel createProductLikeModel(Long userId, Long productId) {
        return ProductLikeModel.create(userId, productId);
    }

    public static ProductLikeModel createProductLikeWithUserId(Long userId) {
        return createProductLikeModel(userId, DEFAULT_PRODUCT_ID);
    }

    public static ProductLikeModel createProductLikeWithProductId(Long productId) {
        return createProductLikeModel(DEFAULT_USER_ID, productId);
    }
}
