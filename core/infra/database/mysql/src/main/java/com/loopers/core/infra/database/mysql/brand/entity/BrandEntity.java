package com.loopers.core.infra.database.mysql.brand.entity;

import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.brand.vo.BrandDescription;
import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.brand.vo.BrandName;
import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Table(
        name = "brand"
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BrandEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public static BrandEntity from(Brand brand) {
        return new BrandEntity(
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
