package com.loopers.domain.product;

/**
 * 상품 정렬 조건
 * - LATEST: 최신순 (생성일 내림차순)
 * - PRICE_ASC: 가격 낮은순
 * - LIKES_DESC: 좋아요 많은순
 */
public enum ProductSortType {
    LATEST,
    PRICE_ASC,
    LIKES_DESC
}
