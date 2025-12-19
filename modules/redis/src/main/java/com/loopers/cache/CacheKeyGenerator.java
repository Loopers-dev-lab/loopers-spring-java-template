package com.loopers.cache;

import java.util.StringJoiner;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * 캐시 키 생성기
 * <p>
 * 일관된 캐시 키 생성을 위한 공통 유틸리티
 *
 * @author hyunjikoh
 * @since 2025. 12. 19.
 */
@Component
public class CacheKeyGenerator {

    private static final String DELIMITER = ":";
    private static final String NULL_VALUE = "null";

    // 캐시 키 프리픽스
    public static final String PRODUCT_PREFIX = "product";
    public static final String DETAIL_PREFIX = "detail";
    public static final String IDS_PREFIX = "ids";
    public static final String PAGE_PREFIX = "page";
    public static final String METRICS_PREFIX = "metrics";
    public static final String POPULAR_PREFIX = "popular";
    public static final String LIST_PREFIX = "list";

    /**
     * 상품 상세 캐시 키: product:detail:{productId}
     */
    public String generateProductDetailKey(Long productId) {
        return new StringJoiner(DELIMITER)
                .add(PRODUCT_PREFIX)
                .add(DETAIL_PREFIX)
                .add(String.valueOf(productId))
                .toString();
    }

    /**
     * 상품 ID 리스트 캐시 키: product:ids:{strategy}:{brandId}:{page}:{size}:{sort}
     */
    public String generateProductIdsKey(CacheStrategy strategy, Long brandId, Pageable pageable) {
        return new StringJoiner(DELIMITER)
                .add(PRODUCT_PREFIX)
                .add(IDS_PREFIX)
                .add(strategy.name().toLowerCase())
                .add(brandId != null ? String.valueOf(brandId) : NULL_VALUE)
                .add(String.valueOf(pageable.getPageNumber()))
                .add(String.valueOf(pageable.getPageSize()))
                .add(generateSortString(pageable.getSort()))
                .toString();
    }

    /**
     * 상품 ID 리스트 패턴: product:ids:{strategy}:*
     */
    public String generateProductIdsPattern(CacheStrategy strategy) {
        return new StringJoiner(DELIMITER)
                .add(PRODUCT_PREFIX)
                .add(IDS_PREFIX)
                .add(strategy.name().toLowerCase())
                .add("*")
                .toString();
    }

    /**
     * 특정 브랜드의 모든 목록 패턴: product:page:{brandId}:*
     */
    public String generateProductListPatternByBrand(Long brandId) {
        return new StringJoiner(DELIMITER)
                .add(PRODUCT_PREFIX)
                .add(PAGE_PREFIX)
                .add(String.valueOf(brandId))
                .add("*")
                .toString();
    }

    /**
     * 인기 상품 캐시 키: product:popular
     */
    public String generatePopularProductsKey() {
        return new StringJoiner(DELIMITER)
                .add(PRODUCT_PREFIX)
                .add(POPULAR_PREFIX)
                .toString();
    }

    /**
     * 상품 목록 캐시 패턴: product:list:*
     */
    public String generateProductListPattern() {
        return new StringJoiner(DELIMITER)
                .add(PRODUCT_PREFIX)
                .add(LIST_PREFIX)
                .add("*")
                .toString();
    }

    /**
     * 상품 메트릭 캐시 키: product:metrics:{productId}
     */
    public String generateProductMetricsKey(Long productId) {
        return new StringJoiner(DELIMITER)
                .add(PRODUCT_PREFIX)
                .add(METRICS_PREFIX)
                .add(String.valueOf(productId))
                .toString();
    }

    /**
     * Sort 객체를 문자열로 변환
     */
    private String generateSortString(Sort sort) {
        if (sort.isUnsorted()) {
            return "unsorted";
        }

        StringJoiner sortJoiner = new StringJoiner(",");
        sort.forEach(order -> {
            String direction = order.getDirection().isAscending() ? "asc" : "desc";
            sortJoiner.add(order.getProperty() + "_" + direction);
        });

        return sortJoiner.toString();
    }

    /**
     * 상품명을 캐시 키에 안전한 형태로 변환
     */
    public String sanitizeProductName(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            return NULL_VALUE;
        }

        String sanitized = productName.trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9가-힣_]", "");

        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }

        return sanitized.isEmpty() ? NULL_VALUE : sanitized;
    }
}
