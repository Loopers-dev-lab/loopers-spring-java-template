package com.loopers.domain.point;

import com.loopers.infrastructure.point.PointJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.testcontainers.MySqlTestContainersConfig;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 포인트 충전 통합 테스트
 * - 존재하지 않는 유저 ID로 충전을 시도한 경우 실패한다.
 * - Testcontainers를 사용하여 실제 MySQL과 통합 테스트 수행
 */
@DisplayName("포인트 충전 통합 테스트")
@SpringBootTest
@Import(MySqlTestContainersConfig.class)
class PointChargeIntegrationTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private PointJpaRepository pointJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("존재하지 않는 유저 ID 로 충전을 시도한 경우, 실패한다.")
    @Test
    void should_fail_when_charging_with_nonexistent_user_id() {
        // given
        String nonExistentUserId = "nonexistent";
        Long chargeAmount = 500L;

        // when
        CoreException exception = assertThrows(CoreException.class, () ->
            pointService.charge(nonExistentUserId, chargeAmount)
        );

        // then
        assertAll(
            () -> assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND),
            () -> assertThat(exception.getMessage()).contains("존재하지 않는 사용자입니다")
        );

        // 포인트 레코드가 생성되지 않았는지 확인
        assertThat(pointJpaRepository.findById(nonExistentUserId)).isEmpty();
    }
}
