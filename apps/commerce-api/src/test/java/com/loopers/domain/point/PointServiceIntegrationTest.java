package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class PointServiceIntegrationTest {

    @Autowired
    private PointService pointService;

    @MockitoSpyBean
    private PointRepository pointRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("포인트 조회를 할 때,")
    @Nested
    class GetPoint {

        @DisplayName("해당 ID의 회원이 존재할 경우, 보유 포인트가 반환된다.")
        @Test
        void returnsPoint_whenUserExists() {
            // arrange
            String userId = "testuser01";
            pointService.createPoint(userId);

            // act
            Point point = pointService.getPoint(userId);

            // assert
            assertAll(
                    () -> assertThat(point).isNotNull(),
                    () -> {
                        Assertions.assertNotNull(point);
                        assertThat(point.getUserId()).isEqualTo(userId);
                    },
                    () -> {
                        Assertions.assertNotNull(point);
                        assertThat(point.getAmount()).isZero();
                    }
            );
        }

        @DisplayName("해당 ID의 회원이 존재하지 않을 경우, null이 반환된다.")
        @Test
        void returnsNull_whenUserDoesNotExist() {
            // act
            Point point = pointService.getPoint("nonexistentuser");

            // assert
            assertThat(point).isNull();
        }
    }

    @DisplayName("포인트 생성을 할 때,")
    @Nested
    class CreatePoint {

        @DisplayName("포인트를 생성할 때 초기 금액은 0이다.")
        @Test
        void createsPointWithZeroAmount() {
            // arrange
            String userId = "testuser01";

            // act
            Point point = pointService.createPoint(userId);

            // assert
            assertAll(
                    () -> assertThat(point).isNotNull(),
                    () -> {
                        Assertions.assertNotNull(point);
                        assertThat(point.getAmount()).isZero();
                    },
                    () -> verify(pointRepository, times(1)).save(any(Point.class))
            );
        }

        @DisplayName("이미 포인트가 존재하는 사용자에 대해 포인트 생성 시, CONFLICT 예외가 발생한다.")
        @Test
        void throwsConflictException_whenPointAlreadyExists() {
            // arrange
            String userId = "testuser01";
            pointService.createPoint(userId);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                pointService.createPoint(userId);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }

        @DisplayName("포인트 생성 성공 시, 데이터베이스에 저장된 Point를 조회할 수 있다.")
        @Test
        void canFindSavedPoint_afterSuccessfulCreation() {
            // arrange
            String userId = "testuser01";

            // act
            pointService.createPoint(userId);

            // assert - 데이터베이스에서 직접 조회
            Point foundPoint = pointRepository.findByUserId(userId)
                    .orElseThrow(() -> new AssertionError("Point should be found"));

            assertAll(
                    () -> assertThat(foundPoint.getUserId()).isEqualTo(userId),
                    () -> assertThat(foundPoint.getAmount()).isZero()
            );
        }
    }

    @DisplayName("포인트 충전을 할 때,")
    @Nested
    class ChargePoint {

        @DisplayName("포인트를 충전할 수 있다.")
        @Test
        void canChargePoint() {
            // arrange
            String userId = "testuser01";
            pointService.createPoint(userId);

            // act
            pointService.chargePoint(userId, 1000L);

            // assert
            Point point = pointService.getPoint(userId);
            assertThat(point.getAmount()).isEqualTo(1000L);
        }

        @DisplayName("존재하지 않는 사용자에게 포인트 충전 시, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFoundException_whenUserDoesNotExist() {
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                pointService.chargePoint("nonexistentuser", 1000L);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }
}
