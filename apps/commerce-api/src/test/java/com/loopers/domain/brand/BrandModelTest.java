package com.loopers.domain.brand;

import com.loopers.application.user.UserCommand;
import com.loopers.domain.user.UserModel;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class BrandModelTest {

    @DisplayName("BrandModel 생성 테스트")
    @Nested
    class Create {

        String brandName;
        String description;
        Character status;

        @BeforeEach
        void setUp() {
            brandName = "test";
            description = "test";
            status = '1';
        }


        @Test
        @DisplayName("유효한 브랜드 이름이 들어오면 BrandModel 생성에 성공한다")
        void create_whenValidBrandNameIsGiven() {
            // act
            BrandModel brand = new BrandModel(brandName, description, status);

            // assert
            assertAll(
                    () -> assertThat(brand.getId()).isNotNull(),
                    () -> assertThat(brand.getName()).isEqualTo(brandName),
                    () -> assertThat(brand.getDescription()).isEqualTo(description),
                    () -> assertThat(brand.getStatus()).isEqualTo(status)
            );
        }

        @Test
        @DisplayName("브랜드 이름이 50자 초과되면 에러를 반환한다.")
        void throwsBadException_whenTooLongBrandNameIsGiven() {
            // given
            brandName = "a".repeat(51);

            // when
            CoreException result = assertThrows(CoreException.class, () -> {
                new BrandModel(brandName, description, status);
            });

            // then
            AssertionsForClassTypes.assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            AssertionsForClassTypes.assertThat(result.getMessage()).isEqualTo("브랜드 이름은 50자 이하 입력값 입니다.");
        }

        @Test
        @DisplayName("브랜드 이름이 null이면 에러를 반환한다.")
        void throwsBadException_whenBrandNameIsNull() {
            // given
            brandName = "";

            // when
            CoreException result = assertThrows(CoreException.class, () -> {
                new BrandModel(brandName, description, status);
            });

            // then
            AssertionsForClassTypes.assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("브랜드 이름이 공백이면 에러를 반환한다.")
        void throwsBadException_whenBrandNameIsSpace() {
            // given
            brandName = " ";

            // when
            CoreException result = assertThrows(CoreException.class, () -> {
                new BrandModel(brandName, description, status);
            });

            // then
            AssertionsForClassTypes.assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

    }
}
