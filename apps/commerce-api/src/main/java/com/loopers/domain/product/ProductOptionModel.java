package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.embeded.AdditionalPrice;
import com.loopers.domain.product.embeded.OptionName;
import com.loopers.domain.product.embeded.OptionValue;
import com.loopers.domain.product.embeded.ProductId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;

@Entity
@Getter
@Table(name = "product_option")
public class ProductOptionModel extends BaseEntity {
    
    @Embedded
    private ProductId productId;
    
    @Embedded
    private OptionName name;
    
    @Embedded
    private OptionValue value;
    
    @Embedded
    private AdditionalPrice additionalPrice;

    public ProductOptionModel() {
    }

    public ProductOptionModel(ProductId productId, OptionName name, OptionValue value, AdditionalPrice additionalPrice) {
        this.productId = productId;
        this.name = name;
        this.value = value;
        this.additionalPrice = additionalPrice;
    }
    
    public static ProductOptionModel create(Long productId, String name, String value, BigDecimal additionalPrice) {
        return new ProductOptionModel(
                ProductId.of(productId),
                OptionName.of(name),
                OptionValue.of(value),
                AdditionalPrice.of(additionalPrice)
        );
    }

    public BigDecimal calculateTotalPrice(BigDecimal basePrice) {
        if (basePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "기본 가격은 0보다 커야 합니다.");
        }
        return basePrice.add(this.additionalPrice.getValue());
    }

    public boolean isValid() {
        return this.productId != null 
            && this.name != null 
            && this.value != null 
            && this.additionalPrice != null
            && this.productId.getValue() > 0
            && !this.name.getValue().trim().isEmpty()
            && !this.value.getValue().trim().isEmpty();
    }

    @Override
    protected void guard() {
        if (!isValid()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 상품 옵션입니다.");
        }
    }
}
