package com.loopers.application.user;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User 통합 테스트")
@SpringBootTest
public class UserFacadeTest {

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private UserService userService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {databaseCleanUp.truncateAllTables();}

    final String validLoginId = "bobby34";
    final String validEmail = "bobby34@naver.com";
    final String validBirthday = "1994-04-08";
    final Gender validGender = Gender.MALE;
    final BigDecimal validPoint = BigDecimal.valueOf(0);

    @DisplayName("User 엔티티 생성")
    @Nested
    class CreateUserTest {

        @DisplayName("성공 케이스 : User 저장에 성공하는지 확인")
        @Test
        void saveUser_withValidFields_Success() {
            // arrange
            UserInfo userinfo = UserInfo.builder()
                    .loginId(validLoginId)
                    .email(validEmail)
                    .birthday(validBirthday)
                    .gender(validGender)
                    .build();

            // act
            UserInfo userInfo = userFacade.saveUser(userinfo);

            // assert
            // 저장한 User 의 내용 일치되는지 확인
            assertEquals(validLoginId, userInfo.loginId());
            assertEquals(validEmail, userInfo.email());
            assertEquals(validBirthday, userInfo.birthday());
            assertEquals(validGender, userInfo.gender());
            assertTrue(validPoint.compareTo(userInfo.point().getAmount()) == 0);
        }

        @DisplayName("실패 케이스 : 이미 저장된 User 를 다시 저장하면 실패")
        @Test
        void saveUser_spyUserRepository_Success() {
            // arrange
            UserInfo userinfo = UserInfo.builder()
                    .loginId(validLoginId)
                    .email(validEmail)
                    .birthday(validBirthday)
                    .gender(validGender)
                    .build();

            userFacade.saveUser(userinfo);

            // act
            CoreException result = assertThrows(CoreException.class
                    , () -> userFacade.saveUser(userinfo));

            // assert
            assertEquals(ErrorType.CONFLICT, result.getErrorType());
            assertEquals("이미 존재하는 회원을 다시 저장 못합니다.", result.getCustomMessage());
        }

    }

    @DisplayName("User 엔티티 조회")
    @Nested
    class GetUserTest {

        @DisplayName("성공 케이스 : ID 의 User가 존재할 경우, 회원 정보 반환")
        @Test
        void getUser_idExists_Success() {
            // arrange
            UserInfo userinfo = UserInfo.builder()
                    .loginId(validLoginId)
                    .email(validEmail)
                    .birthday(validBirthday)
                    .gender(validGender)
                    .build();
            UserInfo userInfo = userFacade.saveUser(userinfo);
            // act

            // assert
            assertNotNull(userInfo);
            assertAll(
                    () -> assertEquals(userInfo.loginId(), validLoginId)
                    , () -> assertEquals(userInfo.email(), validEmail)
                    , () -> assertEquals(userInfo.birthday(), validBirthday)
                    , () -> assertEquals(userInfo.gender(), validGender)
            );
        }

        @DisplayName("실패 케이스 : ID 의 User가 존재하지 않을 경우, Null 반환")
        @Test
        void getUser_idNotExists_getNull() {
            // arrange

            // act
            CoreException result = assertThrows(CoreException.class
                    , () -> userFacade.getUser(validLoginId));

            // assert
            assertEquals(ErrorType.NOT_FOUND, result.getErrorType());
            assertTrue(result.getCustomMessage().endsWith("User 를 찾지 못했습니다."));
        }
    }

}
