package com.loopers.infrastructure.cache;

import java.util.StringJoiner;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * 캐시 키 생성기
 * 
 * Hot/Warm/Cold 전략별 캐시 키 생성
 * - product:detail:{productId}
 * - product:ids:{strategy}:{brandId}:{page}:{size}:{sort}
 * - product:page:{brandId}:{productName}:{page}:{size}:{sort}
 */
@Component
public class CacheKeyGenerator {

    private static final String DELIMITER = ":";
    private static final String NULL_VALUE = "null";

    // 캐시 키 프리픽스
    private static final String PRODUCT_PREFIX = "product";
    private static final String DETAIL_PREFIX = "detail";
    private static final String IDS_PREFIX = "ids";
    private static final String PAGE_PREFIX = "page";

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
     * ID만 캐싱하여 개별 상품 변경 시 전체 캐시 무효화 방지
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
     * 특정 브랜드의 모든 목록 패턴 (레거시): product:page:{brandId}:*
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
     * 상품명을 캐시 키에 안전한 형태로 변환 (공백→언더스코어, 특수문자 제거, 최대 50자)
     */
    private String sanitizeProductName(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            return NULL_VALUE;
        }

        // 공백을 언더스코어로 변환하고, 특수문자 제거
        String sanitized = productName.trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9가-힣_]", "");

        // 최대 50자로 제한
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }

        return sanitized.isEmpty() ? NULL_VALUE : sanitized;
    }

    /**
     * Sort 객체를 문자열로 변환 (예: "likeCount_desc,id_asc")
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
}
