package com.loopers.domain.product;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 상품 상세 정보 Value Object.
 * <p>
 * 상품 상세 조회 시 Product, Brand 정보, 좋아요 수를 조합한 결과를 나타냅니다.
 * 값으로 식별되며 불변성을 가집니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Getter
@EqualsAndHashCode
public class ProductDetail {
    private final Long id;
    private final String name;
    private final Integer price;
    private final Integer stock;
    private final Long brandId;
    private final String brandName;
    private final Long likesCount;

    private ProductDetail(Long id, String name, Integer price, Integer stock, Long brandId, String brandName, Long likesCount) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.brandId = brandId;
        this.brandName = brandName;
        this.likesCount = likesCount;
    }

    /**
     * Create a ProductDetail value object from the provided product fields.
     *
     * @param brandName the display name of the brand
     * @param likesCount the number of likes associated with the product
     * @return a new ProductDetail populated with the supplied values
     */
    public static ProductDetail of(Long id, String name, Integer price, Integer stock, Long brandId, String brandName, Long likesCount) {
        return new ProductDetail(id, name, price, stock, brandId, brandName, likesCount);
    }

    /**
     * Create a ProductDetail from a Product, a brand name, and a likes count.
     *
     * Uses the provided brandName (rather than a Brand aggregate) to avoid cross-aggregate references.
     *
     * @param product   the Product entity to extract id, name, price, stock, and brandId from
     * @param brandName the brand's name to include in the ProductDetail
     * @param likesCount the number of likes to include in the ProductDetail
     * @return the created ProductDetail instance
     * @throws IllegalArgumentException if product, brandName, or likesCount is null
     */
    public static ProductDetail from(Product product, String brandName, Long likesCount) {
        if (product == null) {
            throw new IllegalArgumentException("상품은 null일 수 없습니다.");
        }
        if (brandName == null) {
            throw new IllegalArgumentException("브랜드 이름은 필수입니다.");
        }
        if (likesCount == null) {
            throw new IllegalArgumentException("좋아요 수는 null일 수 없습니다.");
        }

        return ProductDetail.of(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getStock(),
            product.getBrandId(),
            brandName,
            likesCount
        );
    }
}
