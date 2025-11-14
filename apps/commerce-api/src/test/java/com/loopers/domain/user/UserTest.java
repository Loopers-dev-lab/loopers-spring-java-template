package com.loopers.domain.user;

import com.loopers.domain.point.Point;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User 테스트")
public class UserTest {

    @DisplayName("User 엔티티 생성")
    @Nested
    class CreateUserTest {

        final String validLoginId = "bobby34";
        final String validEmail = "bobby34@naver.com";
        final String validBirthday = "1994-04-08";
        final Gender validGender = Gender.MALE;
        final BigDecimal validPoint = BigDecimal.valueOf(0);

        @DisplayName("성공 케이스 : 필드가 모두 형식에 맞으면 User 객체 생성 성공")
        @Test
        void createUser_withValidFields_Success() {
            // arrange
            Point point = Point.builder()
                    .user(null)
                    .amount(validPoint)
                    .build();

            // act
            User user = User.builder()
                    .loginId(validLoginId)
                    .email(validEmail)
                    .birthday(validBirthday)
                    .gender(validGender)
                    .point(point)
                    .build();

            // assert
            assertNotNull(user);
            assertAll(
                    () -> assertEquals(user.getLoginId(), validLoginId)
                    , () -> assertEquals(user.getEmail(), validEmail)
                    , () -> assertEquals(user.getBirthday(), validBirthday)
                    , () -> assertEquals(user.getGender(), validGender)
                    , () -> assertEquals(user.getPoint().getAmount(), validPoint)
            );
        }

        @DisplayName("User ID 형식 검사: 영문, 숫자만 있는 10자 이내 문자열")
        @Nested
        class IdTest {

            @DisplayName("실패 케이스 : User ID가 영문, 숫자 이외 문자열이면 User 객체 생성 실패")
            @Test
            void createUser_withNoEngAndNumId_BadRequest() {
                // arrange
                String noEngAndNumId = "안녕하세요";

                // act
                CoreException result = assertThrows(CoreException.class,
                        () -> User.builder()
                            .loginId(noEngAndNumId)
                            .email(validEmail)
                            .birthday(validBirthday)
                            .gender(validGender)
                            .build()
                );
                
                // assert
                assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
                assertEquals("User : ID가 비어있을 수 없습니다.ID에 영문과 숫자만 10자 이내로 꼭 포함되어 있어야 합니다.", result.getCustomMessage());
            }

            @DisplayName("실패 케이스 : User ID에 영문이 없으면 User 객체 생성 실패")
            @Test
            void createUser_withNoEngId_BadRequest() {
                // arrange
                String noEngId = "34";

                // act
                CoreException result = assertThrows(CoreException.class,
                        () -> User.builder()
                            .loginId(noEngId)
                            .email(validEmail)
                            .birthday(validBirthday)
                            .gender(validGender)
                            .build()
                );

                // assert
                assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
                assertEquals("User : ID가 비어있을 수 없습니다.ID에 영문과 숫자만 10자 이내로 꼭 포함되어 있어야 합니다.", result.getCustomMessage());
            }

            @DisplayName("실패 케이스 : User ID에 숫자가 없으면 User 객체 생성 실패")
            @Test
            void createUser_withNoNumId_BadRequest() {
                // arrange
                String noNumId = "bobby";

                // act
                CoreException result = assertThrows(CoreException.class,
                        () -> User.builder()
                            .loginId(noNumId)
                            .email(validEmail)
                            .birthday(validBirthday)
                            .gender(validGender)
                            .build()
                );

                // assert
                assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
                assertEquals("User : ID가 비어있을 수 없습니다.ID에 영문과 숫자만 10자 이내로 꼭 포함되어 있어야 합니다.", result.getCustomMessage());
            }

            @DisplayName("실패 케이스 : User ID가 10자를 넘어가면 User 객체 생성 실패")
            @Test
            void createUser_withIdOverTenChars_BadRequest() {
                // arrange
                String idOverTenChars = "bobby343434";

                // act
                CoreException result = assertThrows(CoreException.class,
                        () -> User.builder()
                            .loginId(idOverTenChars)
                            .email(validEmail)
                            .birthday(validBirthday)
                            .gender(validGender)
                            .build()
                );

                // assert
                assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
                assertEquals("User : ID가 비어있을 수 없습니다.ID에 영문과 숫자만 10자 이내로 꼭 포함되어 있어야 합니다.", result.getCustomMessage());
            }
        }

        @DisplayName("User email 형식 검사 : `xx@yy.zz`")
        @Nested
        class EmailTest {

            @DisplayName("실패 케이스 : User email에 `@` 이 없으면 User 객체 생성 실패")
            @Test
            void createUser_withNoAtSignEmail_BadRequest() {
                // arrange
                String email = "bobby34naver.com";

                // act
                CoreException result = assertThrows(CoreException.class,
                        () -> User.builder()
                            .loginId(validLoginId)
                            .email(email)
                            .birthday(validBirthday)
                            .gender(validGender)
                            .build()
                );

                // assert
                assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
                assertEquals("User : ID가 비어있을 수 없습니다.email이 `xx@yy.zz` 형식에 맞아야 합니다.", result.getCustomMessage());
            }

            @DisplayName("실패 케이스 : User email에 `@` 이 두 개 있으면 User 객체 생성 실패")
            @Test
            void createUser_withTwoAtSignEmail_BadRequest() {
                // arrange
                String email = "bobby34@@naver.com";

                // act
                CoreException result = assertThrows(CoreException.class,
                        () -> User.builder()
                            .loginId(validLoginId)
                            .email(email)
                            .birthday(validBirthday)
                            .gender(validGender)
                            .build()
                );

                // assert
                assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
                assertEquals("User : ID가 비어있을 수 없습니다.email이 `xx@yy.zz` 형식에 맞아야 합니다.", result.getCustomMessage());
            }

            @DisplayName("실패 케이스 : User email에 `.` 이 없으면 User 객체 생성 실패")
            @Test
            void createUser_withNoDotEmail_BadRequest() {
                // arrange
                String email = "bobby34@navercom";

                // act
                CoreException result = assertThrows(CoreException.class,
                        () -> User.builder()
                            .loginId(validLoginId)
                            .email(email)
                            .birthday(validBirthday)
                            .gender(validGender)
                            .build()
                );

                // assert
                assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
                assertEquals("User : ID가 비어있을 수 없습니다.email이 `xx@yy.zz` 형식에 맞아야 합니다.", result.getCustomMessage());
            }

            @DisplayName("실패 케이스 : User email에 `.` 이 두 개 있으면 User 객체 생성 실패")
            @Test
            void createUser_withTwoDotEmail_BadRequest() {
                // arrange
                String email = "bobby34@naver..com";

                // act
                CoreException result = assertThrows(CoreException.class,
                        () -> User.builder()
                            .loginId(validLoginId)
                            .email(email)
                            .birthday(validBirthday)
                            .gender(validGender)
                            .build()
                );

                // assert
                assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
                assertEquals("User : ID가 비어있을 수 없습니다.email이 `xx@yy.zz` 형식에 맞아야 합니다.", result.getCustomMessage());
            }
        }

        @DisplayName("User birthday 형식 검사 : `yyyy-MM-dd`")
        @Nested
        class BirthdayTest {

            @DisplayName("실패 케이스 : User birthday가 `yyyyMMdd` 형식이면 User 객체 생성 실패")
            @Test
            void createUser_withNoHyphenBirthday_BadRequest() {
                // arrange
                String noHyphenBirthday = "19940408";

                // act
                CoreException result = assertThrows(CoreException.class,
                        () -> User.builder()
                            .loginId(validLoginId)
                            .email(validEmail)
                            .birthday(noHyphenBirthday)
                            .gender(validGender)
                            .build()
                );

                // assert
                assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
                assertEquals("User : ID가 비어있을 수 없습니다.birthday가 `yyyy-MM-dd` 형식에 맞아야 합니다.", result.getCustomMessage());
            }

            @DisplayName("실패 케이스 : User birthday가 `yyyy-MM-d` 형식이면 User 객체 생성 실패")
            @Test
            void createUser_withNoOneDayBirthdayFormat_BadRequest() {
                // arrange
                String birthday = "1994-04-1";

                // act
                CoreException result = assertThrows(CoreException.class,
                        () -> User.builder()
                            .loginId(validLoginId)
                            .email(validEmail)
                            .birthday(birthday)
                            .gender(validGender)
                            .build()
                );

                // assert
                assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
                assertEquals("User : ID가 비어있을 수 없습니다.birthday가 `yyyy-MM-dd` 형식에 맞아야 합니다.", result.getCustomMessage());
            }
        }

    }
}
