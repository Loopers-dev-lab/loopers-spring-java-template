package com.loopers.core.domain.brand;

import com.loopers.core.domain.brand.vo.BrandDescription;
import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.brand.vo.BrandName;
import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import lombok.Getter;

@Getter
public class Brand {

    private final BrandId brandId;

    private final BrandName name;

    private final BrandDescription description;

    private final CreatedAt createdAt;

    private final UpdatedAt updatedAt;

    private final DeletedAt deletedAt;

    private Brand(
            BrandId brandId,
            BrandName name,
            BrandDescription description,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        this.brandId = brandId;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }
}
