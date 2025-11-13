package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.like.ProductLike;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    @Column(name = "stock", nullable = false, columnDefinition = "int default 0")
    private int stock = 0;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "like_count", nullable = false, columnDefinition = "int default 0")
    private Long likeCount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @OneToMany(mappedBy = "likeProduct")
    private List<ProductLike> productLikes = new ArrayList<>();

    @Builder
    protected Product(String productCode, String productName, int stock, BigDecimal price, Brand brand) {

        validationProductCode(productCode);

        validationProductName(productName);

        validationProductPrice(price);

        validationProductStock(stock);

        validationBrand(brand);

        this.productCode = productCode;
        this.productName = productName;
        this.stock = stock;
        this.price = price;
        this.brand = brand;
    }

    public static Product createProduct(String productCode, String productName, BigDecimal price, int stock, Brand brand) {
        return Product.builder()
                .productCode(productCode)
                .productName(productName)
                .price(price)
                .stock(stock)
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

    private static void validationProductPrice(BigDecimal price) {
        if( price == null ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품의 가격은 필수입니다.");
        }

        if(price.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 가격은 0보다 큰 정수여야 합니다");
        }
    }

    private static void validationProductStock(int stock) {

        if( stock < 0 ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 재고는 음수일 수 없습니다");
        }
    }

    private static void validationBrand(Brand brand) {
        if (brand == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드는 필수입니다");
        }
    }

    public void increaseStock(int increase) {
        if (increase <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고 증가량은 양수여야 합니다");
        }
        this.stock += increase;
    }

    public void decreaseStock(int decrease) {
        if (decrease <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고 감소량은 양수여야 합니다");
        }

        if (this.stock < decrease) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다. 현재 재고: " + this.stock);
        }

        this.stock -= decrease;
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
