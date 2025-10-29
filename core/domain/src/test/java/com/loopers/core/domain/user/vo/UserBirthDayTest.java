package com.loopers.core.domain.user.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserBirthDay")
class UserBirthDayTest {

    @Nested
    @DisplayName("create() 메서드")
    class CreateMethod {

        @Nested
        @DisplayName("유효한 값인 경우")
        class 유효한_값인_경우 {

            @Test
            @DisplayName("UserBirthDay 객체를 생성한다")
            void 객체_생성() {
                // given
                String validBirthDay = "2000-01-15";

                // when
                UserBirthDay birthDay = UserBirthDay.create(validBirthDay);

                // then
                assertThat(birthDay.value()).isEqualTo(LocalDate.parse(validBirthDay));
            }

            @ParameterizedTest
            @MethodSource("validBirthDays")
            @DisplayName("유효한 yyyy-MM-dd 형식의 생년월일로 생성한다")
            void 유효한_생년월일(String dateString) {
                // when
                UserBirthDay birthDay = UserBirthDay.create(dateString);

                // then
                assertThat(birthDay.value()).isEqualTo(LocalDate.parse(dateString));
            }

            static Stream<String> validBirthDays() {
                return Stream.of(
                        "1990-01-01",
                        "2000-12-31",
                        "2023-06-15",
                        "1980-02-29"  // 윤년
                );
            }
        }

        @Nested
        @DisplayName("value가 null인 경우")
        class value가_null인_경우 {

            @Test
            @DisplayName("NullPointerException을 발생시킨다")
            void 예외_발생() {
                // given
                String dateString = null;

                // when & then
                assertThatThrownBy(() -> UserBirthDay.create(dateString))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessage("생년월일는(은) Null이 될 수 없습니다.");
            }
        }

        @Nested
        @DisplayName("value가 yyyy-MM-dd 형식이 아닌 경우")
        class value가_형식이_아닌_경우 {

            @ParameterizedTest
            @MethodSource("invalidBirthDays")
            @DisplayName("예외를 발생시킨다")
            void 예외_발생(String dateString) {
                // when & then
                assertThatThrownBy(() -> UserBirthDay.create(dateString))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("생년월일은 yyyy-MM-dd 형식이어야 합니다.");
            }

            static Stream<String> invalidBirthDays() {
                return Stream.of(
                        "2000-1-15",                // MM이 1자
                        "2000-01-5",                // dd가 1자
                        "00-01-15",                 // yyyy가 2자
                        "2000/01/15",               // / 사용
                        "2000.01.15",               // . 사용
                        "20000115",                 // 구분자 없음
                        "01-15-2000",               // MM-dd-yyyy 순서
                        "invalid-date",             // 숫자 아님
                        "2000-13-01",               // 월 범위 초과
                        "2000-01-32"                // 일 범위 초과
                );
            }
        }
    }
}