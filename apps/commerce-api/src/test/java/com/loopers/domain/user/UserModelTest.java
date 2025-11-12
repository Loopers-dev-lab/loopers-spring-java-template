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
    @DisplayName("회원가입 시 User 객체를 생성할 때, ")
    @Nested
    class Create {

        /*
        - [ ]  ID 가 `영문 및 숫자 10자 이내` 형식에 맞지 않으면, User 객체 생성에 실패한다.
        - [ ]  이메일이 `xx@yy.zz` 형식에 맞지 않으면, User 객체 생성에 실패한다.
        - [ ]  생년월일이 `yyyy-MM-dd` 형식에 맞지 않으면, User 객체 생성에 실패한다.
         */

        //입력한 아이디가 빈칸칸이거나 공백이면, User 객체 생성에 실패한다.
        @DisplayName("입력한 ID 가 비어있으면, User 객체 생성에 실패한다.")
        @Test
        void createsUserModel_whenUserIdIsBlank() {
            // arrange
            String userId = "   ";

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserModel(new UserId(userId), new Email("user123@example.com"), new Gender("male"), new BirthDate("1999-01-01"));
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }


        @DisplayName("ID 가 `영문 및 숫자 10자 이내` 형식에 맞지 않으면, User 객체 생성에 실패한다.")
        @Test
        void createUserModel_whenUserIdIsNotValid() {
            // arrange
            String userId = "user123123123";

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserModel(new UserId(userId), new Email("user123@example.com"), new Gender("male"), new BirthDate("1999-01-01"));
            });

            //assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

        }


        //입력한 이메일이 빈칸이거나 공백이면, User 객체 생성에 실패한다.
        @DisplayName("입력한 이메일이 비어있으면, User 객체 생성에 실패한다.")
        @Test
        void createsUserModel_whenEmailIsBlank() {
            // arrange
            String email = "   ";

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserModel(new UserId("userId"), new Email(email), new Gender("male"), new BirthDate("1999-01-01"));
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("이메일이 `xx@yy.zz` 형식에 맞지 않으면, User 객체 생성에 실패한다.")
        @Test
        void createUserModel_whenEmailIsNotValid() {
            // arrange
            String email = "user123123123";

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserModel(new UserId("user123"), new Email(email), new Gender("male"), new BirthDate("1999-01-01"));
            });

            //assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

        }

        @DisplayName("생년월일이 `yyyy-MM-dd` 형식에 맞지 않으면, User 객체 생성에 실패한다.")
        @Test
        void createUserModel_whenBirthDateIsNotValid() {
            // arrange
            String birthDate = "19991-01-01";

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserModel(new UserId("user123"), new Email("user123@user.com"), new Gender("male"), new BirthDate(birthDate));
            });

            //assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

        }


        //입력한 생년월일이 빈칸이거나 공백이면, User 객체 생성에 실패한다.
        @DisplayName("입력한 생년월일이 null이면, User 객체 생성에 실패한다.")
        @Test
        void createsUserModel_whenBirthDateIsNull() {
            // arrange
            String birthDate = null;
            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserModel(new UserId("userId"), new Email("user123@example.com"), new Gender("male"), new BirthDate(birthDate));
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

    }
}
