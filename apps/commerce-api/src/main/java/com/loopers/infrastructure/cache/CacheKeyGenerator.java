package com.loopers.infrastructure.cache;

import com.loopers.domain.product.enums.ProductSortCondition;

public class CacheKeyGenerator {

    private static final String VERSION = "v1";

    // Product detail cache key
    public static String productDetailKey(Long productId) {
        return String.format("product:detail:%s:%d", VERSION, productId);
    }

    // Top liked products (global) cache key
    public static String topLikedProductsKey() {
        return String.format("products:top-liked:global:%s", VERSION);
    }

    // Brand top products cache key
    public static String brandTopProductsKey(Long brandId, int limit) {
        return String.format("products:top-liked:brand:%s:%d:%d", VERSION, brandId, limit);
    }

    // Brand top products pattern for deletion
    public static String brandTopProductsPattern(Long brandId) {
        return String.format("products:top-liked:brand:%s:%d:*", VERSION, brandId);
    }

    // Product list cache key
    public static String productListKey(Long brandId, ProductSortCondition sort, int page, int size) {
        String brandParam = brandId != null ? String.valueOf(brandId) : "all";
        String sortParam = sort != null ? sort.name() : "DEFAULT";
        return String.format("products:list:%s:brand=%s:sort=%s:page=%d:size=%d",
                VERSION, brandParam, sortParam, page, size);
    }

    // Member likes cache key
    public static String memberLikesKey(Long memberId) {
        return String.format("likes:member:%s:%d", VERSION, memberId);
        // 예: "likes:member:v1:123"
    }

    // Product like count cache key
    public static String productLikeCountKey(Long productId) {
        return String.format("product:like:count:%s:%d", VERSION, productId);
        // 예: "product:like:count:v1:456"
    }

    // Product like count cache key pattern (for scanning)
    public static String productLikeCountKeyPattern() {
        return String.format("product:like:count:%s:*", VERSION);
    }

    // Daily ranking ZSET key
    public static String dailyRankingKey(String date) {
        return String.format("ranking:all:%s:%s", VERSION, date);
        // 예: "ranking:all:v1:20251223"
    }
}
