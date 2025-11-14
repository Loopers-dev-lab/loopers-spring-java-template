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
    private Long likeCount;
    @Enumerated(EnumType.STRING)
    private ProductStatus status;
    private Boolean isVisible;
    private Boolean isSellable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @OneToOne(mappedBy = "product")
    private Stock stock;

    @Builder
    private Product(
        String name
        , String description
        , BigDecimal price
        , Long likeCount
        , ProductStatus status
        , Boolean isVisible
        , Boolean isSellable
        , Brand brand
        , Stock stock
    ) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.likeCount = likeCount;
        this.status = status;
        this.isVisible = isVisible;
        this.isSellable = isSellable;
        this.brand = brand;
        this.stock = stock;
        guard();
    }

    // 브랜드 필드를 세팅 (초기화 위해 있음)
    public void setBrand(Brand brand) {
        if(this.brand != null) {
            throw new CoreException(ErrorType.CONFLICT, "Product : Brand 가 이미 존재합니다.");
        }
        this.brand = brand;
    }

    // 재고 필드를 세팅 (초기화 위해 있음)
    public void setStock(Stock stock) {
        if(this.stock != null) {
            throw new CoreException(ErrorType.CONFLICT, "Product : Stock 가 이미 존재합니다.");
        }
        this.stock = stock;
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
    }
}
