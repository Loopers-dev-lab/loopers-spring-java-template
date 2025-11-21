package com.loopers.application.point;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.domain.user.Gender;
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

@DisplayName("PointFacade 통합 테스트")
@SpringBootTest
public class PointFacadeTest {

    @Autowired
    private PointFacade pointFacade;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    final String validLoginId = "bobby34";
    final String validEmail = "bobby34@naver.com";
    final String validBirthday = "1994-04-08";
    final Gender validGender = Gender.MALE;

    @DisplayName("getPoint 테스트")
    @Nested
    class GetPointTest {

        @DisplayName("성공 케이스: 존재하는 User의 포인트 조회 성공")
        @Test
        void getPoint_withValidUser_Success() {
            // arrange
            UserInfo userInfo = UserInfo.builder()
                    .loginId(validLoginId)
                    .email(validEmail)
                    .birthday(validBirthday)
                    .gender(validGender)
                    .build();
            userFacade.saveUser(userInfo);

            // act
            PointInfo result = pointFacade.getPoint(validLoginId);

            // assert
            assertNotNull(result);
            assertEquals(0, BigDecimal.ZERO.compareTo(result.amount()), "초기 포인트는 0이어야 함");
        }

        @DisplayName("실패 케이스: 존재하지 않는 User의 포인트 조회 시 NOT_FOUND 예외 발생")
        @Test
        void getPoint_withNonExistentUser_NotFound() {
            // arrange
            String nonExistentLoginId = "nonexistent";

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    pointFacade.getPoint(nonExistentLoginId)
            );

            assertEquals(ErrorType.NOT_FOUND, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("[loginId = " + nonExistentLoginId + "] Point를 찾을 수 없습니다."));
        }
    }

    @DisplayName("charge 테스트")
    @Nested
    class ChargeTest {

        @DisplayName("성공 케이스: 존재하는 User의 포인트 충전 성공")
        @Test
        void charge_withValidUser_Success() {
            // arrange
            UserInfo userInfo = UserInfo.builder()
                    .loginId(validLoginId)
                    .email(validEmail)
                    .birthday(validBirthday)
                    .gender(validGender)
                    .build();
            userFacade.saveUser(userInfo);

            BigDecimal chargeAmount = BigDecimal.valueOf(1000);
            BigDecimal expectedAmount = BigDecimal.valueOf(1000);

            // act
            PointInfo result = pointFacade.charge(validLoginId, chargeAmount);

            // assert
            assertNotNull(result);
            assertEquals(0, expectedAmount.compareTo(result.amount()), "충전된 포인트가 일치해야 함");
        }

        @DisplayName("성공 케이스: 여러 번 충전 시 누적되어 저장됨")
        @Test
        void charge_multipleCharges_accumulates() {
            // arrange
            UserInfo userInfo = UserInfo.builder()
                    .loginId(validLoginId)
                    .email(validEmail)
                    .birthday(validBirthday)
                    .gender(validGender)
                    .build();
            userFacade.saveUser(userInfo);

            BigDecimal firstCharge = BigDecimal.valueOf(1000);
            BigDecimal secondCharge = BigDecimal.valueOf(2000);
            BigDecimal expectedAmount = BigDecimal.valueOf(3000);

            // act
            pointFacade.charge(validLoginId, firstCharge);
            PointInfo result = pointFacade.charge(validLoginId, secondCharge);

            // assert
            assertNotNull(result);
            assertEquals(0, expectedAmount.compareTo(result.amount()), "충전된 포인트가 누적되어야 함");
        }

        @DisplayName("실패 케이스: 존재하지 않는 User의 포인트 충전 시 NOT_FOUND 예외 발생")
        @Test
        void charge_withNonExistentUser_NotFound() {
            // arrange
            String nonExistentLoginId = "nonexistent";
            BigDecimal chargeAmount = BigDecimal.valueOf(1000);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    pointFacade.charge(nonExistentLoginId, chargeAmount)
            );

            assertEquals(ErrorType.NOT_FOUND, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("포인트를 충전할 Point 객체를 찾을 수 없습니다."));
        }
    }
}
