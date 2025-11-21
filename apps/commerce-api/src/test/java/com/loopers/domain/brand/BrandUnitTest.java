package com.loopers.domain.brand;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;

/**
 * @author hyunjikoh
 * @since 2025. 11. 8.
 */
class BrandUnitTest {

    @org.junit.jupiter.api.Test
    @DisplayName("유효한 정보로 BrandEntity 객체를 생성할 수 있다")
    void create_brand_entity_success() {
        String name = "Test Brand";
        String description = "This is a test brand.";
        BrandDomainCreateRequest brandDomainCreateRequest = new BrandDomainCreateRequest(name, description);
        BrandEntity brandEntity = BrandEntity.createBrandEntity(brandDomainCreateRequest);

        assertEquals(name, brandEntity.getName());
        assertEquals(description, brandEntity.getDescription());
    }

    @org.junit.jupiter.api.Test
    @DisplayName("브랜드 설명이 null 이여도 생성에 성공한다")
    void create_brand_entity_no_desc_success() {
        // given
        String name = "Test Brand";
        String description = null;
        BrandDomainCreateRequest brandDomainCreateRequest = new BrandDomainCreateRequest(name, description);

        BrandEntity brandEntity = BrandEntity.createBrandEntity(brandDomainCreateRequest);

        assertEquals(name, brandEntity.getName());
        assertNull(brandEntity.getDescription());
    }

    @org.junit.jupiter.api.Test
    @DisplayName("브랜드 이름이 null 이면 생성에 실패한다")
    void create_brand_entity_no_name_fail() {
        // given
        String name = null;
        String description = "This is a test brand.";
        BrandDomainCreateRequest brandDomainCreateRequest = new BrandDomainCreateRequest(name, description);

        assertThatThrownBy(() -> {
            BrandEntity.createBrandEntity(brandDomainCreateRequest);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("브랜드 이름은 필수 입력값입니다.");
    }

}
