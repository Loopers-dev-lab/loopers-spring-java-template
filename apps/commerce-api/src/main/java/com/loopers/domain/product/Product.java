package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;

@Entity
@Table(name = "products")
@Getter
public class Product extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long brandId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "price", column = @Column(name = "price", nullable = false, unique = true))
    })
    private Price price;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "count", column = @Column(name = "like_count", nullable = false, unique = true))
    })
    private LikeCount likeCount;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "quantity", column = @Column(name = "stock", nullable = false, unique = true))
    })
    private Stock stock;

    private boolean isDeleted;

    @Builder
    private Product(String name, Long brandId, Price price, LikeCount likeCount, Stock stock) {
        validate(name, brandId, price, likeCount, stock);
        this.name = name;
        this.brandId = brandId;
        this.price = price;
        this.likeCount = likeCount;
        this.stock = stock;
        this.isDeleted = false;
    }

    public Product() {
    }

    public static Product createProduct(String name, Long brandId, Price price, LikeCount likeCount, Stock stock) {
        return Product.builder()
                .name(name)
                .brandId(brandId)
                .price(price)
                .likeCount(likeCount)
                .stock(stock)
                .build();
    }

    public void reduceStock(Integer quantity) {
        this.stock.reduceQuantity(quantity);
    }

    public void increaseLikeCount() {
        this.likeCount.increase();
    }

    public void decreaseLikeCount() {
        this.likeCount.decrease();
    }

    private void validate(String name, Long brandId, Price price, LikeCount likeCount, Stock stock) {
        validateName(name);
        validateBrandId(brandId);
        validatePrice(price);
        validateLikeCount(likeCount);
        validateStock(stock);
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품명은 필수입니다.");
        }
    }

    private void validateBrandId(Long brandId) {
        if (brandId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 ID는 필수입니다.");
        }
    }

    private void validatePrice(Price price) {
        if (price == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 필수입니다.");
        }
    }

    private void validateLikeCount(LikeCount likeCount) {
        if (likeCount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "좋아요 수는 필수입니다.");
        }
    }

    private void validateStock(Stock stock) {
        if (stock == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고는 필수입니다.");
        }
    }
}
