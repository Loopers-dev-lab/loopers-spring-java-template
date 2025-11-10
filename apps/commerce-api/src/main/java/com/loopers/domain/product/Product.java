package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.like.ProductLike;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @OneToMany(mappedBy = "likeProduct")
    private List<ProductLike> productLikes = new ArrayList<>();

    @Builder
    protected Product(String productCode, String productName, int stock, BigDecimal price) {

        validationProductCode(productCode);

        validationProductName(productName);

        validationProductPrice(price);

        this.productCode = productCode;
        this.productName = productName;
        this.stock = stock;
        this.price = price;
    }

    private static void validationProductCode(String productCode) {
        if( productCode == null || productCode.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 코드 오류");
        }
    }

    private static void validationProductName(String productName) {
        if( productName == null || productName.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 이름 오류");
        }
    }

    private static void validationProductPrice(BigDecimal price) {
        if(price.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 가격 오류");
        }
    }

    private void incrementLikeCount() {
        this.likeCount++;
    }

    private void decrementLikeCount() {
        this.likeCount--;
    }


}
