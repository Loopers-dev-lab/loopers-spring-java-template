package com.loopers.domain.like.fixture;

import com.loopers.domain.like.brand.BrandLikeModel;

public class BrandLikeFixture {

    public static final Long DEFAULT_USER_ID = 1L;
    public static final Long DEFAULT_BRAND_ID = 1L;

    public static BrandLikeModel createBrandLikeModel() {
        return BrandLikeModel.create(DEFAULT_USER_ID, DEFAULT_BRAND_ID);
    }

    public static BrandLikeModel createBrandLikeModel(Long userId, Long brandId) {
        return BrandLikeModel.create(userId, brandId);
    }

    public static BrandLikeModel createBrandLikeWithUserId(Long userId) {
        return createBrandLikeModel(userId, DEFAULT_BRAND_ID);
    }

    public static BrandLikeModel createBrandLikeWithBrandId(Long brandId) {
        return createBrandLikeModel(DEFAULT_USER_ID, brandId);
    }
}
