package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserModelTest {
    @DisplayName("UserModel 생성 테스트")
    @Nested
    class Create {
        @DisplayName("이용자ID, 이메일, 생년월일, 이용자이름, 성별 입력값이 유효하면 User 객체 생성에 성공한다")
        @Test
        void createsExampleModel_whenAllRequiredFieldsAreProvidedAndValid() {
            // arrange
            String userId = "ajchoi0928";
            String userName = "junho";
            String description = "loopers backend developer";
            String email = "loopers@loopers.com";
            String birthDate = "1997-09-28";
            String gender = "M";

            // act
            UserModel userModel = new UserModel(userId, userName, description, email, birthDate, gender);

            // assert
            assertAll(
                    () -> assertThat(userModel.getId()).isNotNull(),
                    () -> assertThat(userModel.getUserName()).isEqualTo(userName),
                    () -> assertThat(userModel.getDescription()).isEqualTo(description),
                    () -> assertThat(userModel.getDescription()).isEqualTo(description),
                    () -> assertThat(userModel.getEmail()).isEqualTo(email),
                    () -> assertThat(userModel.getBirthDate()).isEqualTo(birthDate),
                    () -> assertThat(userModel.getGender()).isEqualTo(gender),
                    () -> assertThat(userModel.getPoint()).isEqualTo(0)
            );
        }

        @DisplayName("이용자ID가 10자를 초과했을 때 BAD_REQUEST를 반환한다")
        @Test
        void throwsBadRequestException_whenUserIdLengthOverThanExpected() {
            // arrange
            // arrange
            String userId = "ajchoi12345";
            String userName = "junho";
            String description = "loopers backend developer";
            String email = "loopers@loopers.com";
            String birthDate = "1997-09-28";
            String gender = "M";

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserModel(userId, userName, description, email, birthDate, gender);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("이용자ID가 영문 또는 숫자 외 문자를 포함할 때 BAD_REQUEST를 반환한다")
        @Test
        void throwsBadRequestException_whenUserIdContainsInvalidCharacters() {
            // arrange
            // arrange
            String userId = "ajchoi_97";
            String userName = "junho";
            String description = "loopers backend developer";
            String email = "loopers@loopers.com";
            String birthDate = "1997-09-28";
            String gender = "M";

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserModel(userId, userName, description, email, birthDate, gender);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("이용자ID가 빈 경우 BAD_REQUEST를 반환한다")
        @Test
        void throwsBadRequestException_whenUserIdNotValid() {
            // arrange
            String userId = " ";
            String userName = "junho";
            String description = "loopers backend developer";
            String email = "loopers@loopers.com";
            String birthDate = "1997-09-28";
            String gender = "M";

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserModel(userId, userName, description, email, birthDate, gender);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("이메일 형식이 xx@yy.zz에 맞지 않으면 BAD_REQUEST를 반환한다")
        @Test
        void throwsBadRequestException_whenEmailNotValid() {
            // arrange
            String userId = "ajchoi0928";
            String userName = "junho";
            String description = "loopers backend developer";
            String email = "xy@looperscom";
            String birthDate = "1997-09-28";
            String gender = "M";

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserModel(userId, userName, description, email, birthDate, gender);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("이메일이 빈 경우 BAD_REQUEST를 반환한다")
        @Test
        void throwsBadRequestException_whenEmailIsBlank() {
            // arrange
            String userId = "ajchoi0928";
            String userName = "junho";
            String description = "loopers backend developer";
            String email = "";
            String birthDate = "1997-09-28";
            String gender = "M";

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserModel(userId, userName, description, email, birthDate, gender);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("생년월일이 yyyy-MM-dd 포맷이 아니면 BAD_REQUEST를 반환한다")
        @Test
        void throwsBadRequestException_whenBirthDateNotValid() {
            // arrange
            String userId = "ajchoi0928";
            String userName = "junho";
            String description = "loopers backend developer";
            String email = "ajchoi0928@loopers.com";
            String birthDate = "1997/09/28";
            String gender = "M";

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserModel(userId, userName, description, email, birthDate, gender);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("생년월일이 빈 경우 BAD_REQUEST를 반환한다")
        @Test
        void throwsBadRequestException_whenBirthDateIsBlank() {
            // arrange
            String userId = "ajchoi0928";
            String userName = "junho";
            String description = "loopers backend developer";
            String email = "ajchoi0928@loopers.com";
            String birthDate = "";
            String gender = "M";

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserModel(userId, userName, description, email, birthDate, gender);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
        // user_name, 성별,
        @DisplayName("회원 이름이 빈 경우 BAD_REQUEST를 반환한다")
        @Test
        void throwsBadRequestException_whenUserNameIsBlank() {
            // arrange
            String userId = "ajchoi0928";
            String userName = "";
            String description = "loopers backend developer";
            String email = "ajchoi0928@loopers.com";
            String birthDate = "1997-09-28";
            String gender = "M";

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserModel(userId, userName, description, email, birthDate, gender);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("성별이 빈 경우 BAD_REQUEST를 반환한다")
        @Test
        void throwsBadRequestException_whenGenderIsBlank() {
            // arrange
            String userId = "ajchoi0928";
            String userName = "최준호";
            String description = "loopers backend developer";
            String email = "ajchoi0928@loopers.com";
            String birthDate = "1997-09-28";
            String gender = "";

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserModel(userId, userName, description, email, birthDate, gender);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }


    }
}
