package com.loopers.application.brand;

import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandStatus;

public class BrandCommand {
    public record Create (
            String name,
            String description,
            BrandStatus status
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
