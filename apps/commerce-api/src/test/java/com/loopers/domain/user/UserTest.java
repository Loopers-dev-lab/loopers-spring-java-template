package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorMessages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTest {

    private static final String VALID_EMAIL = "user@example.com";
    private static final String VALID_BIRTH = "1990-01-01";
    private static final String VALID_GENDER = "MALE";

    @Nested
    @DisplayName("ID 형식 검증")
    class IdValidation {
        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {
                "",
                "toolongid11",
                "invalid!"
        })
        void invalid_id_should_throw(String id) {
            CoreException ex = assertThrows(CoreException.class, () ->
                    User.builder()
                            .id(id)
                            .email(VALID_EMAIL)
                            .birthDate(VALID_BIRTH)
                            .gender(VALID_GENDER)
                            .build()
            );
            assert ex.getMessage().contains(ErrorMessages.INVALID_ID_FORMAT);
        }
    }

    @Nested
    @DisplayName("이메일 형식 검증")
    class EmailValidation {
        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {
                "", "abc", "a@b", "a@b.", "a@b.c"
        })
        void invalid_email_should_throw(String email) {
            CoreException ex = assertThrows(CoreException.class, () ->
                    User.builder()
                            .id("user1")
                            .email(email)
                            .birthDate(VALID_BIRTH)
                            .gender(VALID_GENDER)
                            .build()
            );
            assert ex.getMessage().contains(ErrorMessages.INVALID_EMAIL_FORMAT);
        }
    }

    @Nested
    @DisplayName("생년월일 형식 검증")
    class BirthDateValidation {
        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {
                "", "1990/01/01", "19900101", "1990-13-01"
        })
        void invalid_birth_should_throw(String birth) {
            CoreException ex = assertThrows(CoreException.class, () ->
                    User.builder()
                            .id("user1")
                            .email(VALID_EMAIL)
                            .birthDate(birth)
                            .gender(VALID_GENDER)
                            .build()
            );
            assert ex.getMessage().contains(ErrorMessages.INVALID_BIRTH_FORMAT);
        }
    }
}
