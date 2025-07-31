package com.loopers.domain.product.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class BrandId {
    @Column(name = "brand_id")
    private Long brandId;

    public BrandId() {

    }

    private BrandId(Long brandId) {
        this.brandId = brandId;
    }
    public static BrandId of(Long brandId) {
        if(brandId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "brandId is not null");
        }
        return new BrandId(brandId);
    }

    public Long getValue() {
        return brandId;
    }
}
