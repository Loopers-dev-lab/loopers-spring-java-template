package com.loopers.domain.brand.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;

@Embeddable
public class BrandName {

    private String Name;

    private static final String REGEX = "^[a-zA-Z0-9가-힣\\s]*$";
    public BrandName() {

    }
    private BrandName(String brandName) {
        this.Name = brandName;
    }
    public static BrandName of(String brandName) {
        if(brandName == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 이름은 필수 입니다");
        }
        if (!brandName.matches(REGEX)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 이름에 특수 문자는 입력할 수 없습니다.");
        }
       return new BrandName(brandName);
    }

    public String getValue() {
        return Name;
    }
}
