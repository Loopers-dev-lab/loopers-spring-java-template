package com.loopers.domain.members;

import com.loopers.domain.members.enums.Gender;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {

    @DisplayName("Member 객체를 생성 시,")
    @Nested
    class CreateMemberTest {

        @DisplayName("정상 값이 들어오면 Member 객체 생성에 성공한다")
        @Test
        void shouldSaveMember_whenRequestIsValid() {
            Member member = new Member("test123", "test@example.com", "password123", "1990-01-01", Gender.MALE);

            assertThat(member).isNotNull();
            assertThat(member.getMemberId()).isEqualTo("test123");
            assertThat(member.getEmail()).isEqualTo("test@example.com");
            assertThat(member.getBirthDate()).isEqualTo(java.time.LocalDate.of(1990, 1, 1));
            assertThat(member.getGender()).isEqualTo(Gender.MALE);
        }

        @ParameterizedTest
        @ValueSource(strings = {"abcdefghijklmn", "abc123daas456"})
        @DisplayName("Member ID가 10자 초과이면 예외가 발생한다")
        void shouldThrowException_whenMemberIdTooLong(String invalidMemberId) {
            assertThatThrownBy(() ->
                    new Member(invalidMemberId, "test@example.com", "password", "2002-02-02", Gender.FEMALE)
            ).isInstanceOf(CoreException.class)
                    .hasMessageContaining("ID는 영문 및 숫자 10자 이내여야 합니다.");
        }

        @ParameterizedTest
        @ValueSource(strings = {"test@", "@example.com", "test@example"})
        @DisplayName("이메일이 xx@yy.zz 형식에 맞지 않으면, Member 객체 생성에 실패한다")
        void shouldThrowException_whenEmailFormatIsInvalid(String invalidEmail) {
            assertThatThrownBy(() ->
                    new Member("test123", invalidEmail, "password", "1990-01-01", Gender.MALE)
            ).isInstanceOf(CoreException.class)
                    .hasMessageContaining("이메일은 xx@yy.zz 형식이어야 합니다.");
        }

        @ParameterizedTest
        @ValueSource(strings = {"19900101", "1990/01/01", "90-01-01", "1990-13-01", "1990-01-32"})
        @DisplayName("생년월일이 yyyy-MM-dd 형식에 맞지 않으면, Member 객체 생성에 실패한다")
        void shouldThrowException_whenBirthDateFormatIsInvalid(String invalidBirthDate) {
            assertThatThrownBy(() ->
                    new Member("test123", "test@example.com", "password", invalidBirthDate, Gender.MALE)
            ).isInstanceOf(CoreException.class)
                    .hasMessageContaining("생년월일은 yyyy-MM-dd 형식이어야 합니다.");
        }

    }
}
