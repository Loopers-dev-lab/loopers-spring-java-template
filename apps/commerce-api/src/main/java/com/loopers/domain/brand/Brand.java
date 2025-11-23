package com.loopers.domain.brand;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "brands")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Brand extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    private Brand(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public static Brand create(String name, String description) {
        validateName(name);
        validateDescription(description);
        return new Brand(name, description);
    }

    private static void validateDescription(String description) {
        if(description == null) {
            throw new IllegalArgumentException("브랜드 설명이 null일 수 없습니다.");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("브랜드명이 입력되지 않았습니다.");
        }
    }
}
