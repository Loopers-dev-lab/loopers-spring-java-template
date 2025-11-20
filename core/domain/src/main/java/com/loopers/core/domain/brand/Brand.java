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

    private final BrandId id;

    private final BrandName name;

    private final BrandDescription description;

    private final CreatedAt createdAt;

    private final UpdatedAt updatedAt;

    private final DeletedAt deletedAt;

    private Brand(
            BrandId id,
            BrandName name,
            BrandDescription description,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Brand create(BrandName name, BrandDescription description) {
        return new Brand(
                BrandId.empty(),
                name,
                description,
                CreatedAt.now(),
                UpdatedAt.now(),
                DeletedAt.empty()
        );
    }

    public static Brand mappedBy(
            BrandId brandId,
            BrandName name,
            BrandDescription description,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        return new Brand(brandId, name, description, createdAt, updatedAt, deletedAt);
    }
}
