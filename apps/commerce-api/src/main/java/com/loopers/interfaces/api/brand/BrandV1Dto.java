package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandInfo;

public class BrandV1Dto {
    public record BrandResponse(String name) {
        public static BrandResponse from(BrandInfo info) {
            return new BrandResponse(info.name());
        }
    }
}

