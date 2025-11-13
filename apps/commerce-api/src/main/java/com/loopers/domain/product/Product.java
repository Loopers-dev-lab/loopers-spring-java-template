package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.Money;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.like.ProductLike;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Entity
@Table(name = "products")
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

    public void increaseStock(int amount) {
        this.stock = this.stock.increase(amount);
    }

    public void decreaseStock(int amount) {
        this.stock = this.stock.decrease(amount);
    }

    public int getStockQuantity() {
        return this.stock.getQuantity();
    }

    public void incrementLikeCount(ProductLike productLike) {
        this.productLikes.add(productLike);
        this.likeCount++;
    }

    public void decrementLikeCount(ProductLike productLike) {
        this.productLikes.remove(productLike);
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }
}
