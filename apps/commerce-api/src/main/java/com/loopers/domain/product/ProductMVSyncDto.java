package com.loopers.domain.product;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MV 동기화용 DTO
 * <p>
 * Product, Brand, Like를 조인하여 한 번의 쿼리로 조회한 결과를 담습니다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 28.
 */
@Getter
@AllArgsConstructor
public class ProductMVSyncDto {
    // 상품 정보
    private Long productId;
    private String productName;
    private String productDescription;
    private BigDecimal originPrice;
    private BigDecimal discountPrice;
    private Integer stockQuantity;
    private ZonedDateTime productUpdatedAt;

    // 브랜드 정보
    private Long brandId;
    private String brandName;
    private ZonedDateTime brandUpdatedAt;

    // 좋아요 정보
    private Long likeCount;
    private ZonedDateTime likeUpdatedAt;

}
