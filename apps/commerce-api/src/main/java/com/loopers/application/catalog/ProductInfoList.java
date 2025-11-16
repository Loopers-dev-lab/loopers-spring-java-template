package com.loopers.application.catalog;

import java.util.List;

/**
 * 상품 목록 조회 결과.
 *
 * @param products 상품 목록 (좋아요 수 포함)
 * @param totalCount 전체 상품 수
 * @param page 현재 페이지 번호
 * @param size 페이지당 상품 수
 */
public record ProductInfoList(
    List<ProductInfo> products,
    long totalCount,
    int page,
    int size
) {
    /**
     * 전체 페이지 수를 계산합니다.
     *
     * @return 전체 페이지 수
     */
    public int getTotalPages() {
        return size > 0 ? (int) Math.ceil((double) totalCount / size) : 0;
    }

    /**
     * 다음 페이지가 있는지 확인합니다.
     *
     * @return 다음 페이지 존재 여부
     */
    public boolean hasNext() {
        return (page + 1) * size < totalCount;
    }

    /**
     * 이전 페이지가 있는지 확인합니다.
     *
     * @return 이전 페이지 존재 여부
     */
    public boolean hasPrevious() {
        return page > 0;
    }
}

