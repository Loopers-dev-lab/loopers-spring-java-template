package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.vo.LikeCount;
import com.loopers.domain.product.vo.Stock;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Table(name = "products")
@Entity
public class Product extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer price;

    @Embedded
    private Stock stock;

    @Column(nullable = false)
    private LikeCount likeCount;

    @Column(nullable = false)
    private Long brandId;

    public static Product create(final String name, final Integer price, final Stock stock, final Long brandId) {
        validateProductFields(name, price, brandId);
        Product product = new Product();
        product.name = name;
        product.price = price;
        product.stock = stock;
        product.likeCount = new LikeCount();
        product.brandId = brandId;
        return product;
    }

    public void likeCountIncrease(int amount) {
        this.likeCount = this.likeCount.increase(amount);
    }

    public void likeCountDecrease(final int amount) {
        this.likeCount = this.likeCount.decrease(amount);
    }

    private static void validateProductFields(final String name, final Integer price, final Long brandId) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다.");
        }
        if (price == null || price < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 0 이상이어야 합니다.");
        }
        if (brandId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드는 비어있을 수 없습니다.");
        }
    }
}
