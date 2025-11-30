package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.order.OrderItem;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "products",
        indexes = {
                @Index(name = "idx_brand_id", columnList = "brand_id"),
                @Index(name = "idx_brand_price", columnList = "brand_id, price_value ASC"),
                @Index(name = "idx_brand_like", columnList = "brand_id, like_count DESC"),
                @Index(name = "idx_like_count", columnList = "like_count DESC"),
                @Index(name = "idx_created_at", columnList = "created_at DESC")
        }
)
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Embedded
    private ProductPrice price;

    @Embedded
    private Stock stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Version
    private Long version;

    private Product(String name, ProductPrice price, Integer stock, Brand brand) {
        validateRequiredFields(name, price, stock, brand);
        this.name = name;
        this.price = price;
        this.stock = Stock.of(stock);
        this.brand = brand;
    }

    public static Product create(String name, Long price, Integer stock, Brand brand) {
        return new Product(name, ProductPrice.of(price), stock, brand);
    }

    private void validateRequiredFields(String name, ProductPrice price, Integer stock, Brand brand) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품명은 필수입니다.");
        }
        if (price == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 필수입니다.");
        }
        if (stock == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고는 필수입니다.");
        }
        if (brand == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드는 필수입니다.");
        }
    }

    public void decreaseStock(Integer quantity) {
        this.stock = this.stock.decrease(quantity);
    }

    public boolean isStockAvailable(Integer quantity) {
        return this.stock.isAvailable(quantity);
    }

    public Integer getStockValue() {
        return this.stock.getValue();
    }

    public Long getPriceValue() {
        return this.price.getValue();
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "좋아요 수는 0보다 작을 수 없습니다.");
        }
        this.likeCount--;
    }
}
