package com.loopers.domain.brand;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "brand")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Brand extends BaseEntity {

    private String name;
    private String description;
    @Enumerated(EnumType.STRING)
    private BrandStatus status;
    private Boolean isVisible;
    private Boolean isSellable;

    @OneToMany(mappedBy = "brand")
    List<Product> products;

    @Builder
    private Brand(
        String name
        , String description
        , BrandStatus status
        , Boolean isVisible
        , Boolean isSellable
        , List<Product> products
    ) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.isVisible = isVisible;
        this.isSellable = isSellable;
        this.products = products;
    }

    // 상품 목록 필드를 세팅 (초기화 위해 있음)
    public void setPoint(List<Product> products) {
        if(this.products != null) {
            throw new CoreException(ErrorType.CONFLICT, "Brand : List<Product> 가 이미 존재합니다.");
        }
        this.products = products;
    }

    // 유효성 검사
    @Override
    protected void guard() {
        // name 유효성 검사
        if(name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Brand : name이 비어있을 수 없습니다.");
        }

        // description nullable

        // status 유효성 검사
        if(status == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Brand : status 가 비어있을 수 없습니다.");
        }

        // isVisible 유효성 검사
        if(isVisible == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Brand : isVisible 가 비어있을 수 없습니다.");
        }

        // isSellable 유효성 검사
        if(isSellable == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Brand : isSellable 가 비어있을 수 없습니다.");
        }
    }

}
