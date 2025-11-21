package com.loopers.domain.brand;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "brand")
@Getter
@NoArgsConstructor
public class Brand extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Builder
    public Brand(String name) {
        this.name = name;
    }
}
