package com.loopers.domain.product;

import java.math.BigDecimal;

public class ProductFixture {

    public static final String PRODUCT_NAME = "Test Product";
    public static final Long PRODUCT_BRAND_ID = 1L;
    public static final BigDecimal PRODUCT_STOCK = new BigDecimal("100");
    public static final BigDecimal PRODUCT_PRICE = new BigDecimal("10000");
    public static final String PRODUCT_DESCRIPTION = "Test Description";
    public static final String PRODUCT_IMG_URL = "http://example.com/image.jpg";
    public static final String PRODUCT_STATUS = "ACTIVE";
    public static final BigDecimal PRODUCT_LIKE_COUNT = new BigDecimal("0");

    public static ProductModel createProductModel() {
        return ProductModel.register(
                PRODUCT_NAME,
                PRODUCT_BRAND_ID,
                PRODUCT_PRICE,
                PRODUCT_STOCK,
                PRODUCT_DESCRIPTION,
                PRODUCT_IMG_URL,
                PRODUCT_STATUS,
                PRODUCT_LIKE_COUNT
                );
    }
    public static ProductModel createProductModel(String name, Long brandId, BigDecimal stock, BigDecimal price, String description, String imgUrl, String status, BigDecimal likeCount) {
        return ProductModel.register(name, brandId, stock, price,  description, imgUrl, status, likeCount);
    }
    public static ProductModel createProductWithName(String name){
        return createProductModel(name, PRODUCT_BRAND_ID, PRODUCT_STOCK, PRODUCT_PRICE, PRODUCT_DESCRIPTION,PRODUCT_IMG_URL,PRODUCT_STATUS,PRODUCT_LIKE_COUNT);
    }
    public static ProductModel createProductWithBrandId(Long brandId){
        return createProductModel(PRODUCT_NAME, brandId, PRODUCT_STOCK, PRODUCT_PRICE, PRODUCT_DESCRIPTION,PRODUCT_IMG_URL,PRODUCT_STATUS,PRODUCT_LIKE_COUNT);
    }
    public static ProductModel createProductWithPrice(BigDecimal price){
        return createProductModel(PRODUCT_NAME, PRODUCT_BRAND_ID, PRODUCT_STOCK, price, PRODUCT_DESCRIPTION,PRODUCT_IMG_URL,PRODUCT_STATUS,PRODUCT_LIKE_COUNT);
    }
    public static ProductModel createProductWithStock(BigDecimal stock){
        return createProductModel(PRODUCT_NAME, PRODUCT_BRAND_ID,  stock, PRODUCT_PRICE, PRODUCT_DESCRIPTION,PRODUCT_IMG_URL,PRODUCT_STATUS,PRODUCT_LIKE_COUNT);
    }
    public static ProductModel createProductWithDescription(String description){
        return createProductModel(PRODUCT_NAME, PRODUCT_BRAND_ID, PRODUCT_STOCK, PRODUCT_PRICE, description,PRODUCT_IMG_URL,PRODUCT_STATUS,PRODUCT_LIKE_COUNT);
    }
    public static ProductModel createProductWithImgUrl(String imgUrl){
        return createProductModel(PRODUCT_NAME, PRODUCT_BRAND_ID, PRODUCT_STOCK, PRODUCT_PRICE, PRODUCT_DESCRIPTION,imgUrl,PRODUCT_STATUS,PRODUCT_LIKE_COUNT);
    }
    public static ProductModel createProductWithStatus(String status){
        return createProductModel(PRODUCT_NAME, PRODUCT_BRAND_ID, PRODUCT_STOCK, PRODUCT_PRICE, PRODUCT_DESCRIPTION,PRODUCT_IMG_URL,status,PRODUCT_LIKE_COUNT);
    }
    public static ProductModel createProductWithLikeCount(BigDecimal likeCount){
        return createProductModel(PRODUCT_NAME, PRODUCT_BRAND_ID, PRODUCT_STOCK, PRODUCT_PRICE, PRODUCT_DESCRIPTION,PRODUCT_IMG_URL,PRODUCT_STATUS,likeCount);
    }
}
