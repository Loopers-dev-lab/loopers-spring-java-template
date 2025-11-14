package com.loopers.domain.product;

/**
 * 상품 상태
 * - ACTIVE: 정상 (판매 중)
 * - SUSPENDED: 중단 (일시적 판매 중단)
 * - DELETED: 삭제
 */
public enum ProductStatus {
    ACTIVE,
    SUSPENDED,
    DELETED
}
