package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.like.ProductLike;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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
    private String productName;
    private int stock;
    private BigDecimal price;
    private Long likeCount;

    @OneToMany(mappedBy = "likeProduct")
    private List<ProductLike> productLike = new ArrayList<>();

    @Builder
    public Product(String productName, int stock, BigDecimal price) {

        if( productName == null || productName.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 이름 오류");
        }

        if(price.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 가격 오류");
        }

        this.productName = productName;
        this.stock = stock;
        this.price = price;
    }

    private void incrementLikeCount() {
        this.likeCount++;
    }

    private void decrementLikeCount() {
        this.likeCount--;
    }


}
