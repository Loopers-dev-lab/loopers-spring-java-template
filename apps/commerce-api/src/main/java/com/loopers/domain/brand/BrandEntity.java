package com.loopers.domain.brand;

import static java.util.Objects.requireNonNull;
import java.util.Objects;

import com.loopers.domain.BaseEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * @author hyunjikoh
 * @since 2025. 11. 8.
 */
@Entity
@Table(name = "brands", indexes = {
        @Index(name = "idx_brand_name", columnList = "name")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BrandEntity extends BaseEntity {
    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    public BrandEntity(String name, String description) {
        requireNonNull(name);
        this.name = name;
        this.description = description;

    }

    /**
     * 브랜드 엔티티를 생성합니다.
     *
     * @param request 브랜드 생성 요청 정보
     * @return 생성된 브랜드 엔티티
     */
    public static BrandEntity createBrandEntity(BrandDomainCreateRequest request) {
        if (Objects.isNull(request)) {
            throw new IllegalArgumentException("브랜드 생성 요청 정보는 필수입니다.");
        }

        if (Objects.isNull(request.name()) || request.name().isBlank()) {
            throw new IllegalArgumentException("브랜드 이름은 필수 입력값입니다.");
        }

        return new BrandEntity(request.name(), request.description());
    }

    @Override
    protected void guard() {
        if (this.name == null || this.name.isBlank()) {
            throw new IllegalStateException("브랜드 이름은 비어있을 수 없습니다.");
        }

        if (this.name.length() > 100) {
            throw new IllegalStateException("브랜드 이름은 100자를 초과할 수 없습니다.");
        }
    }

}
