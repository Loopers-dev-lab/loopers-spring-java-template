package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BrandTest {

    @DisplayName("브랜드 이름은 필수값이다.")
    @Test
    void whenRegisterBrandInvalidName_throwBadRequest() {

        CoreException result = assertThrows(CoreException.class, () -> {
            Brand.createBrand(null);
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("브랜드 이름은 필수입니다");
    }

    @DisplayName("브랜드 최초 등록시 Active 값은 true 이다.")
    @Test
    void whenRegisterBrand_isActiveIsTrue() {
        // given
        Brand brand = Brand.createBrand("Nike");

        // when // then
        assertThat(brand.isActive()).isTrue();
    }

    @DisplayName("브랜드 미사용 처리시 Active 값은 false 이다.")
    @Test
    void whenDeactivateBrand_isActiveIsFalse() {
        // given
        Brand brand = Brand.createBrand("Nike");

        // when
        brand.deactivate();

        // then
        assertThat(brand.isActive()).isFalse();

    }

    @DisplayName("브랜드 사용 처리시 Active 값은 true 이다.")
    @Test
    void whenActivateBrand_isActiveIsTrue() {
        // given
        Brand brand = Brand.createBrand("Nike");

        // when // then
        brand.deactivate();

        assertThat(brand.isActive()).isFalse();

        brand.activate();

        assertThat(brand.isActive()).isTrue();

    }
}
