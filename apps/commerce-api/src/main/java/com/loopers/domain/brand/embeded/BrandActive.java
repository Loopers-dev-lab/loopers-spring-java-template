package com.loopers.domain.brand.embeded;

import jakarta.persistence.Embeddable;

@Embeddable
public class BrandActive {
    private boolean brandisActive;

    private BrandActive(boolean brandisActive) {
        this.brandisActive = brandisActive;
    }

    public BrandActive() {

    }
    public static BrandActive of(boolean brandisActive) {
        return new BrandActive(brandisActive);
    }
}
