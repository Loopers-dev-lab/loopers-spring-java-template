package com.loopers.domain.brand;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;

@Entity
@Table(name = "brands")
@Getter
public class Brand extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    public Brand() {
    }

    @Builder
    public Brand(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public static Brand createBrand(String name, String description) {
        return Brand.builder()
                .name(name)
                .description(description)
                .build();
    }
}
