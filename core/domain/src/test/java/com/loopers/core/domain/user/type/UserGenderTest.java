package com.loopers.core.domain.user.type;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserGenderTest {

    @Nested
    @DisplayName("create() 메서드")
    class CreateMethod {

        @Nested
        @DisplayName("유효한 값인 경우")
        class 유효한_값인_경우 {

            @Test
            @DisplayName("UserGender 객체를 생성한다")
            void 객체_생성() {
                String validGender = "MALE";
                UserGender gender = UserGender.create(validGender);

                assertThat(gender).isEqualTo(UserGender.MALE);
            }
        }

        @Nested
        @DisplayName("value가 null인 경우")
        class value가_null인_경우 {

            @Test
            @DisplayName("NullPointerException을 발생시킨다")
            void 예외_발생() {
                // given
                String gender = null;

                // when & then
                assertThatThrownBy(() -> UserGender.create(gender))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessage("사용자의 성별는(은) Null이 될 수 없습니다.");
            }
        }

        @Nested
        @DisplayName("value가 유효하지 않은 경우")
        class value가_유효하지_않은_경우 {

            @Test
            @DisplayName("IllegalArgumentException을 발생시킨다.")
            void 예외_발생() {
                String gender = "invalid-gender";

                assertThatThrownBy(() -> UserGender.create(gender))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("성별은 MALE 혹은 FEMALE이어야 합니다.");
            }
        }
    }
}
