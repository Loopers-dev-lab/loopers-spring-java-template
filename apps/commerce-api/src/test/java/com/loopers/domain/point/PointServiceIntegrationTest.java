package com.loopers.domain.point;

import com.loopers.infrastructure.point.PointJpaRepository;
import com.loopers.testcontainers.MySqlTestContainersConfig;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 포인트 서비스 통합 테스트
 * - Testcontainers를 사용한 실제 MySQL 통합 테스트
 * - 프로덕션 환경과 동일한 DB로 테스트
 * - Docker Desktop 실행 필요
 */
@DisplayName("포인트 서비스 통합 테스트")
@SpringBootTest
@Import(MySqlTestContainersConfig.class)
class PointServiceIntegrationTest {

    @Autowired
    private PointService pointService;

    @MockitoSpyBean
    private PointRepository pointRepository;

    @Autowired
    private PointJpaRepository pointJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("포인트 조회 시,")
    @Nested
    class GetPointByUserId {

        @DisplayName("해당 ID의 회원이 존재할 경우, 보유 포인트가 반환된다.")
        @Test
        void should_return_point_when_user_exists() {
            // given
            String userId = "testuser1";
            Long initialAmount = 1000L;

            // 포인트 생성
            Point savedPoint = pointJpaRepository.save(Point.create(userId, initialAmount));

            // when
            Point foundPoint = pointService.getPointByUserId(userId);

            // then
            assertAll(
                    () -> assertThat(foundPoint).isNotNull(),
                    () -> assertThat(foundPoint.getUserId()).isEqualTo(userId),
                    () -> assertThat(foundPoint.getAmount()).isEqualTo(initialAmount)
            );

            // spy 검증: findByUserId 메서드가 호출되었는지 확인
            verify(pointRepository, times(1)).findByUserId(userId);
        }

        @DisplayName("해당 ID의 회원이 존재하지 않을 경우, null이 반환된다.")
        @Test
        void should_return_null_when_user_does_not_exist() {
            // given
            String nonExistentUserId = "nonexistent";

            // when
            Point foundPoint = pointService.getPointByUserId(nonExistentUserId);

            // then
            assertThat(foundPoint).isNull();

            // spy 검증: findByUserId 메서드가 호출되었는지 확인
            verify(pointRepository, times(1)).findByUserId(nonExistentUserId);
        }
    }
}
