package com.loopers.core.infra.database.redis.product.entity;

import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.brand.vo.BrandDescription;
import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.brand.vo.BrandName;
import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Optional;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BrandRedisEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public static BrandRedisEntity from(Brand brand) {
        return new BrandRedisEntity(
                Optional.ofNullable(brand.getId().value())
                        .map(Long::parseLong)
                        .orElse(null),
                brand.getName().value(),
                brand.getDescription().value(),
                brand.getCreatedAt().value(),
                brand.getUpdatedAt().value(),
                brand.getDeletedAt().value()
        );
    }

    public Brand to() {
        return Brand.mappedBy(
                new BrandId(this.id.toString()),
                new BrandName(this.name),
                new BrandDescription(this.description),
                new CreatedAt(this.createdAt),
                new UpdatedAt(this.updatedAt),
                new DeletedAt(this.deletedAt)
        );
    }
}
