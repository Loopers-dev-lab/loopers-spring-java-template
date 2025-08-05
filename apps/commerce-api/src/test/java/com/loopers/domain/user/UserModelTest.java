package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
// UserFixture을 작성하면 코드리뷰하는 사람이 이해하기 어려울까요,,?
public class UserModelTest {

    @Nested
    @DisplayName("")
    class create {
        @DisplayName("ID, 이메일, 생년월일, 성별이 모두 주어지면, User 객체를 정상적으로 생성한다.")
        @Test
        void createsUserModel_whenAllFieldsAreProvided() {
            // arrange
            String loginId = UserFixture.USER_LOGIN_ID;
            String email = UserFixture.USER_EMAIL;
            String birth = UserFixture.USER_BIRTH_DATE;
            String gender = UserFixture.USER_GENDER;
            // act
            UserModel userModel = UserModel.register(loginId, email, birth, gender);
            // assert
            assertAll(
                    () -> assertThat(userModel).isNotNull(),
                    () -> assertThat(userModel.getLoginId()).isEqualTo(loginId)
            );

        }

        @DisplayName("ID 가 영문 및 숫자 10자 이내 형식에 맞지 않으면, User 객체 생성에 실패한다.")
        @Test
        void throwsException_whenIdIsInvalid() {
            // arrange
            String loginId = "invalid_id_1234567890";
            //act
            CoreException exception = assertThrows(CoreException.class, () -> {
                UserFixture.createUserWithLoginId(loginId);
            });
            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
        @DisplayName("이메일이 xx@yy.zz 형식에 맞지 않으면, User 객체 생성에 실패한다.")
        @Test
        void throwsException_whenEmailIsInvalid() {
            String email = "test@testnet";
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                UserFixture.createUserWithEmail(email);
            });
            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
        @DisplayName("생년월일이 yyyy-MM-dd 형식에 맞지 않으면, User 객체 생성에 실패한다.")
        @Test
        void throwsException_whenBirthIsInvalid() {
            String birth = "20000804";
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                UserFixture.createUserWithBirthDate(birth);
            });
            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
        @DisplayName("생년월일이 yyyy-MM-dd 형식은 맞지만 없는 날짜거나 미래의 날짜인경우에, User 객체 생성에 실패한다.")
        @Test
        void throwsException_whenBirthIsInvalid_2() {
            String birth = "29999-08-04"; // 미래의 날짜
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                UserFixture.createUserWithBirthDate(birth);
            });
            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
