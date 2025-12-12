package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 브랜드 JPA Entity
 */
@Entity
@Table(name = "brands")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BrandEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BrandStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime modifiedAt;

    // === 생성자 ===

    private BrandEntity(
            String name,
            String description,
            String imageUrl,
            BrandStatus status,
            LocalDateTime createdAt,
            LocalDateTime modifiedAt
    ) {
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.status = status;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    // === Domain <-> Entity 변환 ===

    /**
     * Domain 객체로부터 Entity 생성
     */
    public static BrandEntity from(Brand brand) {
        return new BrandEntity(
                brand.getName(),
                brand.getDescription(),
                brand.getImageUrl(),
                brand.getStatus(),
                brand.getCreatedAt(),
                brand.getModifiedAt()
        );
    }

    /**
     * Entity를 Domain 객체로 변환
     */
    public Brand toDomain() {
        return Brand.reconstitute(
                this.id,
                this.name,
                this.description,
                this.imageUrl,
                this.status,
                this.createdAt,
                this.modifiedAt
        );
    }

    /**
     * Domain 객체로부터 Entity 업데이트
     */
    public void updateFrom(Brand brand) {
        this.name = brand.getName();
        this.description = brand.getDescription();
        this.imageUrl = brand.getImageUrl();
        this.status = brand.getStatus();
        this.modifiedAt = brand.getModifiedAt();
    }
}
