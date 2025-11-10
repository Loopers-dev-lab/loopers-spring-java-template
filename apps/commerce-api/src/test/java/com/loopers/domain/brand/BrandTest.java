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
            Brand brand = new Brand("");
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("브랜드 이름은 필수입니다");
    }
}
