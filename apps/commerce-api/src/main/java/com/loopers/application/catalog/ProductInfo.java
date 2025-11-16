package com.loopers.application.catalog;

import com.loopers.domain.product.ProductDetail;

/**
 * 상품 상세 정보를 담는 레코드.
 *
 * @param productDetail 상품 상세 정보 (Product + Brand + 좋아요 수)
 */
public record ProductInfo(ProductDetail productDetail) {
}

