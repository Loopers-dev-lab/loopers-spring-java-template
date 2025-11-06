package com.loopers.domain.point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.application.user.UserRegisterCommand;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserRepository;
import com.loopers.fixtures.PointTestFixture;
import com.loopers.fixtures.UserTestFixture;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;

/**
 * @author hyunjikoh
 * @since 2025. 10. 29.
 */
@SpringBootTest
@DisplayName("PointService 통합 테스트")
class PointServiceIntegrationTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private UserRepository userRepository;

    @MockitoSpyBean
    private PointHistoryRepository pointHistoryRepository;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("포인트 조회")
    class GetPointTest {

        @Test
        @DisplayName("사용자 등록 시 포인트가 0으로 초기화되어 있다")
        void user_has_zero_point_when_registered() {
            // given
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            // when
            UserEntity user = userRepository.findByUsername(userInfo.username()).orElseThrow();

            // then
            PointTestFixture.assertUserPointIsZero(user);
        }
    }

    @Nested
    @DisplayName("포인트 충전")
    class ChargePointTest {

        @Test
        @DisplayName("존재하지 않는 유저 ID로 충전을 시도하면 실패한다")
        void charge_with_nonexistent_user_fails() {
            // when & then
            assertThatThrownBy(() -> pointService.charge(
                    PointTestFixture.NONEXISTENT_USERNAME,
                    PointTestFixture.CHARGE_AMOUNT_1000))
                    .isInstanceOf(CoreException.class)
                    .hasMessage(PointTestFixture.ERROR_USER_NOT_FOUND);
        }

        @Test
        @DisplayName("존재하는 유저가 1000원을 충전하면 충전된 보유 총량을 반환한다")
        void charge_1000_returns_total_amount() {
            // given
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            // when
            BigDecimal totalAmount = pointService.charge(
                    userInfo.username(),
                    PointTestFixture.CHARGE_AMOUNT_1000);

            // then
            assertThat(totalAmount).isEqualByComparingTo(PointTestFixture.CHARGE_AMOUNT_1000_SCALED);
            
            // 사용자 엔티티에서도 포인트가 업데이트되었는지 확인
            UserEntity user = userRepository.findByUsername(userInfo.username()).orElseThrow();
            PointTestFixture.assertUserPointAmount(user, PointTestFixture.CHARGE_AMOUNT_1000_SCALED);
        }

        @Test
        @DisplayName("포인트 충전 시 포인트 내역이 저장된다")
        void charge_saves_point_history() {
            // given
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            // when
            pointService.charge(userInfo.username(), PointTestFixture.CHARGE_AMOUNT_1000);

            // then
            List<PointHistoryEntity> userPointHistories = pointService.getPointHistories(userInfo.username());
            assertThat(userPointHistories).hasSize(1);

            PointHistoryEntity history = userPointHistories.get(0);
            assertThat(history.getTransactionType()).isEqualTo(PointTransactionType.CHARGE);
            assertThat(history.getAmount()).isEqualByComparingTo(PointTestFixture.CHARGE_AMOUNT_1000);
            assertThat(history.getBalanceAfter()).isEqualByComparingTo(PointTestFixture.CHARGE_AMOUNT_1000_SCALED);
        }
    }
}
