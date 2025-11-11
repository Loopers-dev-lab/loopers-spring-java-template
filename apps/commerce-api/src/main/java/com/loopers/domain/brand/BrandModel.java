package com.loopers.domain.brand;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.ProductModel;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "brand")
public class BrandModel extends BaseEntity {
    @Column(nullable = false,  length = 50)
    private String name;

    private String description;

    @Column(length = 1)
    private Character status;

    protected BrandModel() {}

    public BrandModel(String name, String description, Character status) {
        validateBrandName(name);
        validateBrandStatus(status);

        this.name = name;
        this.description = description;
        this.status = status;
    }

    private void validateBrandName(String name) {
        if(name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 이름은 필수 입력값 입니다.");
        }

        if(name.length() > 50) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 이름은 50자 이하 입력값 입니다.");
        }

        this.name = name;
    }

    /**
     * 1 : 등록
     * 0 : 해제
     * 9 : 관리자 취소
     * @param status
     */
    private void validateBrandStatus(Character status) {
        if(status == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상태값 필수 입력");
        }
        this.status = status;

    }

}
