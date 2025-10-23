package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserModelTest {
    
    @DisplayName("사용자 모델을 생성할 때, ")
    @Nested
    class Create {
        
        @DisplayName("모든 필수 정보가 올바르게 주어지면, 정상적으로 생성된다.")
        @Test
        void createsUserModel_whenAllRequiredFieldsAreProvided() {
            // arrange
            String id = "user123";
            String email = "test@example.com";
            String birthDate = "1990-01-01";
            String gender = "M";

            // act
            UserModel userModel = new UserModel(id, email, birthDate, gender);

            // assert
            assertAll(
                () -> assertThat(userModel.getUserId()).isEqualTo(id),
                () -> assertThat(userModel.getEmail()).isEqualTo(email),
                () -> assertThat(userModel.getBirthDate().toString()).isEqualTo(birthDate),
                () -> assertThat(userModel.getGender()).isEqualTo(gender),
                () -> assertThat(userModel.getPoint()).isEqualTo(0)
            );
        }

        @DisplayName("ID가 10자를 초과하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenIdExceedsTenCharacters() {
            // arrange
            String id = "user1234567"; // 11자

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserModel(id, "test@example.com", "1990-01-01", "M");
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getMessage()).contains("ID는 10자 이내여야 합니다");
        }

        @DisplayName("ID가 영문 및 숫자가 아니면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenIdContainsInvalidCharacters() {
            // arrange
            String id = "user-123"; // 특수문자 포함

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserModel(id, "test@example.com", "1990-01-01", "M");
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getMessage()).contains("ID는 영문 및 숫자만 사용할 수 있습니다");
        }

        @DisplayName("ID가 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenIdIsEmpty() {
            // arrange
            String id = "";

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserModel(id, "test@example.com", "1990-01-01", "M");
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getMessage()).contains("ID는 비어있을 수 없습니다");
        }

        @DisplayName("이메일 형식이 올바르지 않으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenEmailFormatIsInvalid() {
            // arrange
            String email = "invalid-email";

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserModel("user123", email, "1990-01-01", "M");
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getMessage()).contains("이메일 형식이 올바르지 않습니다");
        }

        @DisplayName("이메일이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenEmailIsEmpty() {
            // arrange
            String email = "";

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserModel("user123", email, "1990-01-01", "M");
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getMessage()).contains("이메일은 비어있을 수 없습니다");
        }

        @DisplayName("생년월일 형식이 올바르지 않으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenBirthDateFormatIsInvalid() {
            // arrange
            String birthDate = "1990/01/01"; // 잘못된 형식

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserModel("user123", "test@example.com", birthDate, "M");
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getMessage()).contains("생년월일은 yyyy-MM-dd 형식이어야 합니다");
        }

        @DisplayName("생년월일이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenBirthDateIsEmpty() {
            // arrange
            String birthDate = "";

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserModel("user123", "test@example.com", birthDate, "M");
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getMessage()).contains("생년월일은 비어있을 수 없습니다");
        }
    }

    @DisplayName("포인트 충전할 때, ")
    @Nested
    class ChargePoint {
        
        @DisplayName("0 이하의 정수로 충전하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenChargingZeroOrNegativeAmount() {
            // arrange
            UserModel userModel = new UserModel("user123", "test@example.com", "1990-01-01", "M");

            // act & assert - 0으로 충전
            CoreException result1 = assertThrows(CoreException.class, () -> {
                userModel.chargePoint(0);
            });
            assertThat(result1.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result1.getMessage()).contains("충전할 포인트는 0보다 큰 정수여야 합니다");

            // act & assert - 음수로 충전
            CoreException result2 = assertThrows(CoreException.class, () -> {
                userModel.chargePoint(-100);
            });
            assertThat(result2.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result2.getMessage()).contains("충전할 포인트는 0보다 큰 정수여야 합니다");
        }

        @DisplayName("양수로 충전하면, 포인트가 정상적으로 증가한다.")
        @Test
        void increasesPoint_whenChargingPositiveAmount() {
            // arrange
            UserModel userModel = new UserModel("user123", "test@example.com", "1990-01-01", "M");
            Integer chargeAmount = 1000;

            // act
            userModel.chargePoint(chargeAmount);

            // assert
            assertThat(userModel.getPoint()).isEqualTo(1000);
        }
    }
}
