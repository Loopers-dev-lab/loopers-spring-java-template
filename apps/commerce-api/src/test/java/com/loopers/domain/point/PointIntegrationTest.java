package com.loopers.domain.point;

import com.loopers.application.point.PointFacade;
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

@DisplayName("Point 통합 테스트")
@SpringBootTest
public class PointIntegrationTest {

    @Autowired
    private PointFacade pointFacade;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {databaseCleanUp.truncateAllTables();}

    final String validLoginId = "bobby34";
    final String validEmail = "bobby34@naver.com";
    final String validBirthday = "1994-04-08";
    final Gender validGender = Gender.MALE;
    final BigDecimal validPoint = BigDecimal.valueOf(0);

    @DisplayName("Point 충전 테스트")
    @Nested
    class PointTest {

        @DisplayName("실패 케이스 : 존재하지 않는 User ID 로 충전을 시도한 경우, 실패")
        @Test
        void addPoint_withNoUser_NotFound() {
            // arrange
            BigDecimal requestPoint = BigDecimal.valueOf(1000);

            // act
            CoreException result = assertThrows(CoreException.class,
                    () -> pointFacade.charge(validLoginId,  requestPoint)
            );

            System.out.println("--- CoreException Details ---");
            System.out.println("ErrorType: " + result.getErrorType());
            System.out.println("CustomMessage: " + result.getCustomMessage());
            System.out.println("------------------------");

            // assert
            assertEquals(ErrorType.NOT_FOUND, result.getErrorType());
            assertTrue(result.getCustomMessage().endsWith("포인트를 충전할 Point 객체를 찾을 수 없습니다."));
        }
    }
}
