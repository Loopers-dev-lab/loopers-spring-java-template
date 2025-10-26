package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserId 테스트")
class UserIdTest {

  @DisplayName("UserId를 생성할 때, ")
  @Nested
  class Create {

    @DisplayName("영문+숫자 10자 이내면 정상 생성된다.")
    @Test
    void shouldCreate_whenValid() {
      String validId = "user123";

      UserId result = new UserId(validId);

      assertThat(result)
          .extracting("id")
          .isEqualTo(validId);
    }

    @DisplayName("null 또는 빈 문자열이면 BAD_REQUEST 예외가 발생한다")
    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowBadRequest_whenNullOrEmpty(String invalidId) {
      assertThatThrownBy(() -> new UserId(invalidId))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
          .hasMessageContaining("비어있을 수 없습니다");
    }

    @DisplayName("영문+숫자 10자 이내가 아니면 BAD_REQUEST 예외가 발생한다")
    @ParameterizedTest
    @ValueSource(strings = {
        "abc12345678",    // 11자
        "user@123",       // 특수문자
        "user-name",      // 하이픈
        "user.name",      // 점
        "user한글",        // 한글
        "홍길동",          // 한글만
        "user name"       // 공백
    })
    void shouldThrowBadRequest_whenInvalidFormat(String invalidId) {
      assertThatThrownBy(() -> new UserId(invalidId))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
          .hasMessageContaining("영문 및 숫자 10자 이내");
    }
  }
}
