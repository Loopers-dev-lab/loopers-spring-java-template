package com.loopers.application.brand;

import com.loopers.domain.brand.BrandModel;

public class BrandCommand {
    public record Create (
            String name,
            String description,
            Character status
    ) {
        public BrandModel toModel() {
            return new BrandModel(
                    name,
                    description,
                    status
            );
        }
    }
}
