package com.loopers.domain.point;

import com.loopers.domain.user.Gender;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Point 테스트")
public class PointTest {

    @DisplayName("point 충전 테스트")
    @Nested
    class AddPointTest {

        final String validLoginId = "bobby34";
        final String validEmail = "bobby34@naver.com";
        final String validBirthday = "1994-04-08";
        final Gender validGender = Gender.MALE;
        final BigDecimal validPoint = BigDecimal.valueOf(0);

        @DisplayName("실패 케이스 : 0 이하의 정수로 포인트를 충전 시 실패")
        @Test
        void charge_inputZero_BadRequest() {
            // arrange
            Point point = Point.builder()
                    .user(null)
                    .amount(validPoint)
                    .build();

            BigDecimal requestPoint = BigDecimal.valueOf(0);

            // act
            CoreException result = assertThrows(CoreException.class,
                    () -> point.charge(requestPoint)
            );

            // assert
            assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
            assertEquals("충전할 point는 0 이하가 될 수 없습니다.", result.getCustomMessage());
        }

        @DisplayName("실패 케이스 : 음수로 포인트를 충전 시 실패")
        @Test
        void charge_inputBelowZero_BadRequest() {
            // arrange
            Point point = Point.builder()
                    .user(null)
                    .amount(validPoint)
                    .build();

            BigDecimal requestPoint = BigDecimal.valueOf(-10);

            // act
            CoreException result = assertThrows(CoreException.class,
                    () -> point.charge(requestPoint)
            );

            // assert
            assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
            assertEquals("충전할 point는 0 이하가 될 수 없습니다.", result.getCustomMessage());
        }
    }

    @DisplayName("point 차감 테스트")
    @Nested
    class DeductPointTest {

        final String validLoginId = "bobby34";
        final String validEmail = "bobby34@naver.com";
        final String validBirthday = "1994-04-08";
        final Gender validGender = Gender.MALE;
        final BigDecimal validPoint = BigDecimal.valueOf(10000);

        @DisplayName("성공 케이스 : 정상적인 포인트 차감")
        @Test
        void deduct_validAmount_Success() {
            // arrange
            Point point = Point.builder()
                    .user(null)
                    .amount(validPoint)
                    .build();

            BigDecimal deductAmount = BigDecimal.valueOf(5000);

            // act
            BigDecimal result = point.deduct(deductAmount);

            // assert
            assertEquals(BigDecimal.valueOf(5000), result);
            assertEquals(BigDecimal.valueOf(5000), point.getAmount());
        }

        @DisplayName("실패 케이스 : 0 이하의 정수로 포인트를 차감 시 실패")
        @Test
        void deduct_inputZero_BadRequest() {
            // arrange
            Point point = Point.builder()
                    .user(null)
                    .amount(validPoint)
                    .build();

            BigDecimal requestPoint = BigDecimal.valueOf(0);

            // act
            CoreException result = assertThrows(CoreException.class,
                    () -> point.deduct(requestPoint)
            );

            // assert
            assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
            assertEquals("차감할 point는 0 이하가 될 수 없습니다.", result.getCustomMessage());
        }

        @DisplayName("실패 케이스 : 음수로 포인트를 차감 시 실패")
        @Test
        void deduct_inputBelowZero_BadRequest() {
            // arrange
            Point point = Point.builder()
                    .user(null)
                    .amount(validPoint)
                    .build();

            BigDecimal requestPoint = BigDecimal.valueOf(-10);

            // act
            CoreException result = assertThrows(CoreException.class,
                    () -> point.deduct(requestPoint)
            );

            // assert
            assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
            assertEquals("차감할 point는 0 이하가 될 수 없습니다.", result.getCustomMessage());
        }

        @DisplayName("실패 케이스 : 포인트가 부족한 경우")
        @Test
        void deduct_insufficientPoint_BadRequest() {
            // arrange
            Point point = Point.builder()
                    .user(null)
                    .amount(validPoint)
                    .build();

            BigDecimal requestPoint = BigDecimal.valueOf(20000); // 현재 포인트보다 많은 금액

            // act
            CoreException result = assertThrows(CoreException.class,
                    () -> point.deduct(requestPoint)
            );

            // assert
            assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
            assertTrue(result.getCustomMessage().contains("포인트가 부족합니다"));
        }
    }
}
