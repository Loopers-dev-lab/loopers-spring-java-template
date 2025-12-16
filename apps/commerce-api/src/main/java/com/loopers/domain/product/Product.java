package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.Money;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.like.ProductLike;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@NoArgsConstructor
@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_product_like_count", columnList = "like_count"),
                @Index(name = "idx_brand_like_count", columnList = "brand_id, like_count")
        }
)
@Getter
public class Product extends BaseEntity {

    @Column(name = "product_code", nullable = false, unique = true)
    private String productCode;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Embedded
    private Stock stock;

    @Embedded
    private Money price;

    @Column(name = "like_count", nullable = false, columnDefinition = "int default 0")
    private Long likeCount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @OneToMany(mappedBy = "likeProduct")
    private List<ProductLike> productLikes = new ArrayList<>();

    @Builder
    protected Product(String productCode, String productName, int stockQuantity, Money price, Brand brand) {

        validationProductCode(productCode);

        validationProductName(productName);

        validationBrand(brand);

        validationPrice(price);

        this.productCode = productCode;
        this.productName = productName;
        this.stock = Stock.of(stockQuantity);
        this.price = price;
        this.brand = brand;
    }

    public static Product createProduct(String productCode, String productName, Money price, int stock, Brand brand) {
        return Product.builder()
                .productCode(productCode)
                .productName(productName)
                .price(price)
                .stockQuantity(stock)
                .brand(brand)
                .build();
    }

    private static void validationProductCode(String productCode) {
        if( productCode == null || productCode.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 코드는 필수값입니다");
        }
    }

    private static void validationProductName(String productName) {
        if( productName == null || productName.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 이름은 필수값입니다");
        }
    }

    private static void validationBrand(Brand brand) {
        if (brand == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드는 필수입니다");
        }
    }

    private static void validationPrice(Money price) {
        if (price == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 가격은 필수입니다");
        }
    }

    public void increaseStock(int amount) {
        this.stock = this.stock.increase(amount);
    }

    public void decreaseStock(int amount) {
        this.stock = this.stock.decrease(amount);
    }

    public int getStockQuantity() {
        return this.stock.getQuantity();
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    /**
     * 상품 정보 업데이트
     * @param productName 변경할 상품명 (null이면 변경하지 않음)
     * @param price 변경할 가격 (null이면 변경하지 않음)
     */
    public void updateProductInfo(String productName, Money price) {
        if (productName != null) {
            validationProductName(productName);
            this.productName = productName;
        }
        if (price != null) {
            validationPrice(price);
            this.price = price;
        }
    }

    /**
     * 좋아요 수 동기화 (배치 전용)
     * ProductLike 테이블의 실제 좋아요 수와 동기화
     */
    public void syncLikeCount(long actualCount) {
        if (this.likeCount != actualCount) {
            log.warn("좋아요 수 불일치 감지 - ProductId: {}, 현재: {}, 실제: {}",
                    this.getId(), this.likeCount, actualCount);
            this.likeCount = actualCount;
        }
    }
}
