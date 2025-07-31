package com.loopers.domain.product.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;

@Embeddable
public class ProductName {
    private String name;

    private static final String REGEX = "^[a-zA-Z0-9가-힣\\s]{1,29}$";
    public ProductName() {}
    private ProductName(String name) {
        this.name = name;
    }

    public static ProductName of(String name) {
        if(name == null || !name.matches(REGEX)) {
            throw new CoreException(ErrorType.BAD_REQUEST);
        }
        return new ProductName(name);
    }

    public String getValue() {
        return this.name;
    }
}
