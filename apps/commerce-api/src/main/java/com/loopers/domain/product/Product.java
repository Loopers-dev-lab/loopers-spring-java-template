package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.stock.Stock;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Product extends BaseEntity {

    private String name;
    private String description;
    private BigDecimal price;
    private Long likeCount = 0L;
    @Enumerated(EnumType.STRING)
    private ProductStatus status;
    private Boolean isVisible;
    private Boolean isSellable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Stock stock = Stock.builder().product(this).quantity(0L).build();

    @Builder
    private Product(
        String name
        , String description
        , BigDecimal price
        , ProductStatus status
        , Boolean isVisible
        , Boolean isSellable
        , Brand brand
    ) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.status = status;
        this.isVisible = isVisible;
        this.isSellable = isSellable;
        this.brand = brand;
        guard();
    }

    // 브랜드 필드를 세팅
    public void setBrand(Brand brand) {
        this.brand = brand;
    }

    // 유효성 검사
    @Override
    protected void guard() {
        // name 유효성 검사
        if(name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Product : name이 비어있을 수 없습니다.");
        }

        // description nullable

        // price 유효성 검사
        if(price == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Product : price 가 비어있을 수 없습니다.");
        } else if(price.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Product : price 는 음수가 될 수 없습니다.");
        }

        // likeCount 유효성 검사
        if(likeCount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Product : likeCount 가 비어있을 수 없습니다.");
        } else if(likeCount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Product : likeCount 는 음수가 될 수 없습니다.");
        }

        // status 유효성 검사
        if(status == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Product : status 가 비어있을 수 없습니다.");
        }

        // isVisible 유효성 검사
        if(isVisible == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Product : isVisible 가 비어있을 수 없습니다.");
        }

        // isSellable 유효성 검사
        if(isSellable == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Product : isSellable 가 비어있을 수 없습니다.");
        }

        // stock 유효성 검사
        if(stock == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Product : stock 가 비어있을 수 없습니다.");
        }

        // brand nullable
    }
}
