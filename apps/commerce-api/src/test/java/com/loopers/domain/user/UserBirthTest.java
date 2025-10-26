package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserBirth 테스트")
class UserBirthTest {

  @DisplayName("UserBirth를 생성할 때")
  @Nested
  class Create {

    @DisplayName("yyyy-MM-dd 형식이면 정상 생성한다.")
    @Test
    void shouldCreate_whenValidFormat() {
      String validDate = "1990-01-15";

      UserBirth result = new UserBirth(validDate);

      assertThat(result)
          .extracting("birth")
          .isEqualTo(LocalDate.of(1990, 1, 15));
    }

    @DisplayName("null 또는 빈 문자열이면 BAD_REQUEST 예외가 발생한다")
    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowBadRequest_whenNullOrEmpty(String invalidDate) {
      assertThatThrownBy(() -> new UserBirth(invalidDate))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
          .hasMessageContaining("비어있을 수 없습니다");
    }

    @DisplayName("yyyy-MM-dd 형식이 아니면 BAD_REQUEST 예외가 발생한다")
    @ParameterizedTest
    @ValueSource(strings = {
        "1990/01/15",      // 슬래시 구분자
        "15-01-1990",      // dd-MM-yyyy 형식
        "1990-1-15",       // 월 한자리
        "1990-01-5",       // 일 한자리
        "90-01-15",        // 년도 두자리
        "1990-13-01",      // 잘못된 월
        "1990-01-32",      // 잘못된 일
        "abcd-01-15"       // 문자
    })
    void shouldThrowBadRequest_whenInvalidFormat(String invalidDate) {
      assertThatThrownBy(() -> new UserBirth(invalidDate))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
          .hasMessageContaining("생년월일 형식이 올바르지 않습니다");
    }

  }

}
