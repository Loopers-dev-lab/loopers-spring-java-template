package com.loopers.core.service.brand.query;

import lombok.Getter;

@Getter
public class GetBrandQuery {

    private final String brandId;

    public GetBrandQuery(String brandId) {
        this.brandId = brandId;
    }
}
