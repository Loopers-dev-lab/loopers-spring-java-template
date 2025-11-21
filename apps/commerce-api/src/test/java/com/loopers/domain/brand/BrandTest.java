package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("브랜드(Brand) Entity 테스트")
public class BrandTest {

    @DisplayName("브랜드를 생성할 때, ")
    @Nested
    class Create {
        @DisplayName("정상적인 이름으로 브랜드를 생성할 수 있다. (Happy Path)")
        @Test
        void should_createBrand_when_validName() {
            // arrange
            String brandName = "Nike";

            // act
            Brand brand = Brand.create(brandName);

            // assert
            assertThat(brand.getName()).isEqualTo("Nike");
        }

        @DisplayName("빈 문자열로 브랜드를 생성할 경우, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_emptyName() {
            // arrange
            String brandName = "";

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> Brand.create(brandName));
            assertThat(exception.getMessage()).isEqualTo("브랜드 이름은 필수이며 1자 이상이어야 합니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("null로 브랜드를 생성할 경우, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_nullName() {
            // arrange
            String brandName = null;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> Brand.create(brandName));
            assertThat(exception.getMessage()).isEqualTo("브랜드 이름은 필수이며 1자 이상이어야 합니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("공백만 있는 문자열로 브랜드를 생성할 경우, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_blankName() {
            // arrange
            String brandName = "   ";

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> Brand.create(brandName));
            assertThat(exception.getMessage()).isEqualTo("브랜드 이름은 필수이며 1자 이상이어야 합니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("긴 이름으로도 브랜드를 생성할 수 있다. (Edge Case)")
        @Test
        void should_createBrand_when_longName() {
            // arrange
            String brandName = "A".repeat(1000);

            // act
            Brand brand = Brand.create(brandName);

            // assert
            assertThat(brand.getName()).isEqualTo("A".repeat(1000));
        }
    }

    @DisplayName("브랜드 조회를 할 때, ")
    @Nested
    class Retrieve {
        @DisplayName("생성한 브랜드의 이름을 조회할 수 있다. (Happy Path)")
        @Test
        void should_retrieveName_when_brandCreated() {
            // arrange
            Brand brand = Brand.create("Adidas");

            // act
            String name = brand.getName();

            // assert
            assertThat(name).isEqualTo("Adidas");
        }
    }

    @DisplayName("브랜드 동등성을 확인할 때, ")
    @Nested
    class Equality {
        @DisplayName("같은 이름을 가진 브랜드는 서로 다른 인스턴스이다. (Edge Case)")
        @Test
        void should_beDifferentInstances_when_sameName() {
            // arrange
            String brandName = "Puma";
            Brand brand1 = Brand.create(brandName);
            Brand brand2 = Brand.create(brandName);

            // act & assert
            assertThat(brand1).isNotSameAs(brand2);
            assertThat(brand1).isNotEqualTo(brand2);
        }

        @DisplayName("다른 이름을 가진 브랜드는 서로 다른 인스턴스이다. (Happy Path)")
        @Test
        void should_beDifferentInstances_when_differentNames() {
            // arrange
            Brand brand1 = Brand.create("Nike");
            Brand brand2 = Brand.create("Adidas");

            // act & assert
            assertThat(brand1).isNotSameAs(brand2);
            assertThat(brand1).isNotEqualTo(brand2);
            assertThat(brand1.getName()).isNotEqualTo(brand2.getName());
        }
    }
}
