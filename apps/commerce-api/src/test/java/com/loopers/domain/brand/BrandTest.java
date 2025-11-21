package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * packageName : com.loopers.domain.brand
 * fileName     : BrandTest
 * author      : byeonsungmun
 * date        : 2025. 11. 14.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 14.     byeonsungmun       최초 생성
 */
class BrandTest {

    @DisplayName("Brand 단위 테스트")
    @Nested
    class CreateBrandTest {

        @Test
        @DisplayName("브랜드 생성 성공")
        void createBrandSuccess() {
            Brand brand = Brand.create("Nike");
            assertThat(brand.getName()).isEqualTo("Nike");
        }

        @Test
        @DisplayName("브랜드 이름이 없으면 예외")
        void createBrandFail() {
            assertThatThrownBy(() -> Brand.create(""))
                    .isInstanceOf(CoreException.class);
        }
    }
}
