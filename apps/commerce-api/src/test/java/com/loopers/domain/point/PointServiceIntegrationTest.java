package com.loopers.domain.point;

import com.loopers.domain.common.vo.Money;
import com.loopers.domain.user.User;
import com.loopers.infrastructure.point.PointJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Optional;

import static com.loopers.support.fixture.UserFixtures.createValidUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class PointServiceIntegrationTest {
    @Autowired
    private PointService pointService;

    @MockitoSpyBean
    private PointJpaRepository pointJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("포인트를 조회할 때,")
    @Nested
    class GetPoint {
        @DisplayName("해당 ID 의 회원이 존재할 경우, 보유 포인트가 반환된다.")
        @Test
        void getPoint_whenUserExists() {
            // arrange
            User user = createValidUser();
            userJpaRepository.save(user);

            Point point = Point.create(user.getId());
            pointJpaRepository.save(point);

            // act
            Optional<Money> balance = pointService.findPointBalance(user.getId());

            // assert
            assertThat(balance).isPresent();
            assertThat(balance.get().getAmount()).isEqualByComparingTo("0");
        }

        @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.")
        @Test
        void getPoint_failsWhenUserNotExists() {
            // arrange
            Long invalidId = -1L;

            // act
            Optional<Money> balance = pointService.findPointBalance(invalidId);

            // assert
            assertThat(balance).isEmpty();
        }

    }

    @DisplayName("포인트를 충전할 때,")
    @Nested
    class ChargePoint {

        @DisplayName("존재하지 않는 유저 ID 로 충전을 시도한 경우, 실패한다.")
        @Test
        void chargePoint_failsWhenUserNotExists() {
            // arrange
            Long invalidId = -1L;

            // act
            CoreException result = assertThrows(CoreException.class, () ->
                    pointService.chargePoint(invalidId, Money.of(1000))
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }

        @DisplayName("존재하는 유저 ID 로 충전하면 성공한다.")
        @Test
        void chargePoint_WhenUserExists() {
            // arrange
            User user = createValidUser();
            userJpaRepository.save(user);

            Point point = Point.create(user.getId());
            pointJpaRepository.save(point);

            // act
            Money balance = pointService.chargePoint(user.getId(), Money.of("1000"));

            // assert
            assertThat(balance.getAmount()).isEqualByComparingTo("1000");
        }
    }
}
