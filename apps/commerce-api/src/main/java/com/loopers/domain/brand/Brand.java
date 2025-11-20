package com.loopers.domain.brand;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Table(name = "brands")
@Entity
public class Brand extends BaseEntity {

    @Column(nullable = false)
    private String name;

    public static Brand create(final String name) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다.");
        }
        Brand brand = new Brand();
        brand.name = name;
        return brand;
    }
}
