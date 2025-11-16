package com.loopers.application.pointwallet;

import com.loopers.application.signup.SignUpFacade;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserTestFixture;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@DisplayName("PointWalletFacade 통합 테스트")
class PointWalletFacadeIntegrationTest {
    @Autowired
    private PointWalletFacade pointWalletFacade;

    @Autowired
    private SignUpFacade signUpFacade;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("포인트 조회에 관한 통합 테스트")
    @Nested
    class PointInfo {
        @DisplayName("해당 ID 의 회원이 존재할 경우, 보유 포인트가 반환된다.")
        @ParameterizedTest
        @EnumSource(Gender.class)
        void returnsPoints_whenUserExists(Gender gender) {
            // arrange
            String userId = UserTestFixture.ValidUser.USER_ID;
            String email = UserTestFixture.ValidUser.EMAIL;
            String birthDate = UserTestFixture.ValidUser.BIRTH_DATE;
            signUpFacade.signUp(userId, email, birthDate, gender.name());

            // act
            PointWalletFacade.PointsInfo pointsInfo = pointWalletFacade.getPoints(userId);

            // assert
            assertAll(
                () -> assertThat(pointsInfo).isNotNull(),
                () -> assertThat(pointsInfo.userId()).isEqualTo(userId),
                () -> assertThat(pointsInfo.balance()).isEqualTo(0L)
            );
        }

        @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, 예외가 발생한다.")
        @Test
        void throwsException_whenUserDoesNotExist() {
            // arrange
            String userId = "unknown";

            // act & assert
            assertThatThrownBy(() -> pointWalletFacade.getPoints(userId))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("포인트 충전에 관한 통합 테스트")
    @Nested
    class PointCharge {
        @DisplayName("포인트를 충전할 수 있다.")
        @ParameterizedTest
        @EnumSource(Gender.class)
        void chargesPoints_success(Gender gender) {
            // arrange
            String userId = UserTestFixture.ValidUser.USER_ID;
            String email = UserTestFixture.ValidUser.EMAIL;
            String birthDate = UserTestFixture.ValidUser.BIRTH_DATE;
            signUpFacade.signUp(userId, email, birthDate, gender.name());
            Long chargeAmount = 10_000L;

            // act
            PointWalletFacade.PointsInfo pointsInfo = pointWalletFacade.chargePoint(userId, chargeAmount);

            // assert
            assertAll(
                () -> assertThat(pointsInfo).isNotNull(),
                () -> assertThat(pointsInfo.userId()).isEqualTo(userId),
                () -> assertThat(pointsInfo.balance()).isEqualTo(chargeAmount)
            );
        }

        @DisplayName("사용자가 존재하지 않으면 포인트 충전 시 예외가 발생한다.")
        @Test
        void throwsException_whenUserDoesNotExist() {
            // arrange
            String userId = "unknown";
            Long chargeAmount = 10_000L;

            // act & assert
            assertThatThrownBy(() -> pointWalletFacade.chargePoint(userId, chargeAmount))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND);
        }
    }
}

