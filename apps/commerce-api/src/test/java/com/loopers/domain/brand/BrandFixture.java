package com.loopers.domain.brand;

import java.math.BigDecimal;

public class BrandFixture {

    public static final String BRAND_NAME = "Test Brand";
    public static final String BRAND_SNS_LINK = "https://instagram.com/testbrand";
    public static final BigDecimal BRAND_LIKE_COUNT = new BigDecimal("0");
    public static final boolean BRAND_IS_ACTIVE = true;

    public static BrandModel createBrandModel() {
        return BrandModel.of(
                BRAND_NAME,
                BRAND_SNS_LINK,
                BRAND_LIKE_COUNT,
                BRAND_IS_ACTIVE
        );
    }

    public static BrandModel createBrandModel(String brandName, String snsLink, BigDecimal likeCount, boolean isActive) {
        return BrandModel.of(brandName, snsLink, likeCount, isActive);
    }

    public static BrandModel createBrandWithName(String brandName) {
        return createBrandModel(brandName, BRAND_SNS_LINK, BRAND_LIKE_COUNT, BRAND_IS_ACTIVE);
    }

    public static BrandModel createBrandWithSnsLink(String snsLink) {
        return createBrandModel(BRAND_NAME, snsLink, BRAND_LIKE_COUNT, BRAND_IS_ACTIVE);
    }

    public static BrandModel createBrandWithLikeCount(BigDecimal likeCount) {
        return createBrandModel(BRAND_NAME, BRAND_SNS_LINK, likeCount, BRAND_IS_ACTIVE);
    }

    public static BrandModel createInactiveBrand() {
        return createBrandModel(BRAND_NAME, BRAND_SNS_LINK, BRAND_LIKE_COUNT, false);
    }
    
    public static BrandModel createBrand(String brandName) {
        return createBrandWithName(brandName);
    }
}
