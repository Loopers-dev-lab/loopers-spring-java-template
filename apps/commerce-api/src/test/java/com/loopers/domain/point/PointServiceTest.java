package com.loopers.domain.point;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.domain.user.Gender;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Point Service 단위 테스트")
@SpringBootTest
class PointServiceTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private final String testLoginId = "bobby34";
    private final BigDecimal initialPointAmount = BigDecimal.valueOf(100000);

    @BeforeEach
    void setUp() {
        // 테스트용 User 생성
        UserInfo userInfo = UserInfo.builder()
                .loginId(testLoginId)
                .email(testLoginId + "@test.com")
                .birthday("1990-01-01")
                .gender(Gender.MALE)
                .build();
        userFacade.saveUser(userInfo);

        // 포인트 충전
        pointService.charge(testLoginId, initialPointAmount);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("charge 테스트")
    @Nested
    class ChargeTest {

        @DisplayName("실패 케이스: 0 이하의 정수로 포인트를 충전 시 실패")
        @Test
        void charge_inputZero_BadRequest() {
            // arrange
            BigDecimal requestPoint = BigDecimal.valueOf(0);

            // act
            CoreException result = assertThrows(CoreException.class,
                    () -> pointService.charge(testLoginId, requestPoint)
            );

            // assert
            assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
            assertEquals("포인트 충전량은 0보다 커야 합니다.", result.getCustomMessage());
        }

        @DisplayName("실패 케이스: 음수로 포인트를 충전 시 실패")
        @Test
        void charge_inputBelowZero_BadRequest() {
            // arrange
            BigDecimal requestPoint = BigDecimal.valueOf(-10);

            // act
            CoreException result = assertThrows(CoreException.class,
                    () -> pointService.charge(testLoginId, requestPoint)
            );

            // assert
            assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
            assertEquals("포인트 충전량은 0보다 커야 합니다.", result.getCustomMessage());
        }
    }

    @DisplayName("deduct 테스트")
    @Nested
    class DeductTest {

        @DisplayName("성공 케이스: 정상적인 포인트 차감")
        @Test
        void deduct_validAmount_Success() {
            // arrange
            BigDecimal deductAmount = BigDecimal.valueOf(5000);

            // act
            pointService.deduct(testLoginId, deductAmount);

            // assert
            BigDecimal finalAmount = pointService.findByUserLoginId(testLoginId)
                    .map(Point::getAmount)
                    .orElseThrow(() -> new RuntimeException("포인트를 찾을 수 없습니다"));
            BigDecimal expectedAmount = initialPointAmount.subtract(deductAmount);
            assertEquals(0, finalAmount.compareTo(expectedAmount));
        }

        @DisplayName("실패 케이스: 0 이하의 정수로 포인트를 차감 시 실패")
        @Test
        void deduct_inputZero_BadRequest() {
            // arrange
            BigDecimal requestPoint = BigDecimal.valueOf(0);

            // act
            CoreException result = assertThrows(CoreException.class,
                    () -> pointService.deduct(testLoginId, requestPoint)
            );

            // assert
            assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
            assertEquals("포인트 차감량은 0보다 커야 합니다.", result.getCustomMessage());
        }

        @DisplayName("실패 케이스: 음수로 포인트를 차감 시 실패")
        @Test
        void deduct_inputBelowZero_BadRequest() {
            // arrange
            BigDecimal requestPoint = BigDecimal.valueOf(-10);

            // act
            CoreException result = assertThrows(CoreException.class,
                    () -> pointService.deduct(testLoginId, requestPoint)
            );

            // assert
            assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
            assertEquals("포인트 차감량은 0보다 커야 합니다.", result.getCustomMessage());
        }

        @DisplayName("실패 케이스: 포인트가 부족한 경우")
        @Test
        void deduct_insufficientPoint_BadRequest() {
            // arrange
            BigDecimal requestPoint = initialPointAmount.add(BigDecimal.valueOf(10000)); // 현재 포인트보다 많은 금액

            // act
            CoreException result = assertThrows(CoreException.class,
                    () -> pointService.deduct(testLoginId, requestPoint)
            );

            // assert
            assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
            assertTrue(result.getCustomMessage().contains("포인트가 부족합니다"));
        }
    }

    @DisplayName("deduct 동시성 테스트")
    @Nested
    class DeductConcurrencyTest {

        @DisplayName("성공 케이스: 여러 스레드가 동시에 같은 사용자의 포인트를 차감해도 정확히 차감됨")
        @Test
        void deduct_concurrentRequests_success() throws InterruptedException {
            // arrange
            int threadCount = 10;
            BigDecimal deductAmountPerThread = BigDecimal.valueOf(5000);
            BigDecimal expectedFinalPoint = initialPointAmount.subtract(
                    deductAmountPerThread.multiply(BigDecimal.valueOf(threadCount))
            );

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            List<Exception> exceptions = new ArrayList<>();

            // act
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        pointService.deduct(testLoginId, deductAmountPerThread);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // wait for all threads to complete
            assertTrue(latch.await(10, TimeUnit.SECONDS), "모든 스레드가 10초 내에 완료되지 않았습니다.");
            executor.shutdown();

            // assert
            BigDecimal finalPoint = pointRepository.findByUser_loginId(testLoginId)
                    .map(Point::getAmount)
                    .orElseThrow(() -> new RuntimeException("포인트를 찾을 수 없습니다"));

            assertEquals(0, finalPoint.compareTo(expectedFinalPoint),
                    String.format("최종 포인트는 예상값과 일치해야 함: 예상=%s, 실제=%s", expectedFinalPoint, finalPoint));
            assertEquals(threadCount, successCount.get(),
                    String.format("성공한 스레드 수는 전체 스레드 수와 일치해야 함: 예상=%d, 실제=%d", threadCount, successCount.get()));
            assertEquals(0, failureCount.get(),
                    String.format("실패한 스레드 수는 0이어야 함: 실제=%d", failureCount.get()));
            assertTrue(exceptions.isEmpty(),
                    String.format("예외가 발생하지 않아야 함: 예외 개수=%d", exceptions.size()));
        }

        @DisplayName("성공 케이스: 포인트가 부족한 경우 일부만 성공하고 나머지는 실패함")
        @Test
        void deduct_insufficientPoint_partialSuccess() throws InterruptedException {
            // arrange
            int threadCount = 25; // 포인트 100000원에서 각 5000원씩 차감하면 최대 20개 가능
            BigDecimal deductAmountPerThread = BigDecimal.valueOf(5000);

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            List<Exception> exceptions = new ArrayList<>();

            // act
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        pointService.deduct(testLoginId, deductAmountPerThread);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // wait for all threads to complete
            assertTrue(latch.await(10, TimeUnit.SECONDS), "모든 스레드가 10초 내에 완료되지 않았습니다.");
            executor.shutdown();

            // assert
            BigDecimal finalPoint = pointRepository.findByUser_loginId(testLoginId)
                    .map(Point::getAmount)
                    .orElseThrow(() -> new RuntimeException("포인트를 찾을 수 없습니다"));

            // 최대 20개 차감 가능 (100000 / 5000 = 20)
            int maxPossibleDeductions = initialPointAmount.divide(deductAmountPerThread, RoundingMode.DOWN).intValue();
            assertTrue(successCount.get() <= maxPossibleDeductions,
                    String.format("성공한 차감 수는 최대 가능 차감 수 이하여야 함: 최대=%d, 실제=%d", maxPossibleDeductions, successCount.get()));
            assertEquals(threadCount, successCount.get() + failureCount.get(),
                    String.format("성공 수 + 실패 수는 전체 스레드 수와 일치해야 함: 예상=%d, 실제=%d", threadCount, successCount.get() + failureCount.get()));
            assertTrue(finalPoint.compareTo(BigDecimal.ZERO) >= 0,
                    String.format("최종 포인트는 0 이상이어야 함: 실제=%s", finalPoint));
            assertTrue(finalPoint.compareTo(deductAmountPerThread) < 0,
                    String.format("남은 포인트는 차감 단위보다 작아야 함: 차감 단위=%s, 남은 포인트=%s", deductAmountPerThread, finalPoint));

            // 실패한 경우는 포인트 부족 예외여야 함
            long pointInsufficientExceptions = exceptions.stream()
                    .filter(e -> e instanceof CoreException)
                    .filter(e -> ((CoreException) e).getErrorType() == ErrorType.BAD_REQUEST)
                    .filter(e -> e.getMessage().contains("포인트가 부족"))
                    .count();
            assertEquals(failureCount.get(), pointInsufficientExceptions,
                    String.format("포인트 부족 예외 수는 실패 수와 일치해야 함: 예상=%d, 실제=%d", failureCount.get(), pointInsufficientExceptions));
        }

        @DisplayName("성공 케이스: 포인트 차감이 원자적으로 처리되어 Lost Update가 발생하지 않음")
        @Test
        void deduct_atomicOperation_noLostUpdate() throws InterruptedException {
            // arrange
            int threadCount = 20;
            BigDecimal deductAmountPerThread = BigDecimal.valueOf(1000);

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            // act
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        pointService.deduct(testLoginId, deductAmountPerThread);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // 실패는 무시 (포인트 부족 등)
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // wait for all threads to complete
            assertTrue(latch.await(10, TimeUnit.SECONDS), "모든 스레드가 10초 내에 완료되지 않았습니다.");
            executor.shutdown();

            // assert
            BigDecimal finalPoint = pointRepository.findByUser_loginId(testLoginId)
                    .map(Point::getAmount)
                    .orElseThrow(() -> new RuntimeException("포인트를 찾을 수 없습니다"));

            // Lost Update가 발생하지 않았는지 확인
            // 성공한 차감 수만큼 정확히 차감되어야 함
            BigDecimal expectedPoint = initialPointAmount.subtract(
                    deductAmountPerThread.multiply(BigDecimal.valueOf(successCount.get()))
            );
            assertEquals(0, finalPoint.compareTo(expectedPoint),
                    String.format("Lost Update가 발생하지 않아야 함: 예상 포인트=%s, 실제 포인트=%s, 성공한 차감 수=%d", expectedPoint, finalPoint, successCount.get()));
        }

        @DisplayName("성공 케이스: 동시에 여러 스레드가 포인트를 차감해도 음수가 되지 않음")
        @Test
        void deduct_concurrentRequests_noNegativePoint() throws InterruptedException {
            // arrange
            int threadCount = 25; // 포인트 100000원에서 각 5000원씩 차감하면 최대 20개 가능
            BigDecimal deductAmountPerThread = BigDecimal.valueOf(5000);

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            // act
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        pointService.deduct(testLoginId, deductAmountPerThread);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // wait for all threads to complete
            assertTrue(latch.await(10, TimeUnit.SECONDS), "모든 스레드가 10초 내에 완료되지 않았습니다.");
            executor.shutdown();

            // assert
            BigDecimal finalPoint = pointRepository.findByUser_loginId(testLoginId)
                    .map(Point::getAmount)
                    .orElseThrow(() -> new RuntimeException("포인트를 찾을 수 없습니다"));

            // 포인트가 음수가 되지 않아야 함
            assertTrue(finalPoint.compareTo(BigDecimal.ZERO) >= 0,
                    String.format("포인트가 음수가 되지 않아야 함: 실제 포인트=%s", finalPoint));
            // 성공한 차감 수만큼 정확히 차감되어야 함
            BigDecimal expectedPoint = initialPointAmount.subtract(
                    deductAmountPerThread.multiply(BigDecimal.valueOf(successCount.get()))
            );
            assertEquals(0, finalPoint.compareTo(expectedPoint),
                    String.format("성공한 차감 수만큼 정확히 차감되어야 함: 예상 포인트=%s, 실제 포인트=%s, 성공한 차감 수=%d", expectedPoint, finalPoint, successCount.get()));
        }
    }
}

