package com.loopers.domain.point;

import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserRepository;
import com.loopers.infrastructure.point.PointJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class PointServiceIntegrationTest {
    @Autowired
    private PointService pointService;

    @Autowired
    private PointJpaRepository pointJpaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @SpyBean
    private PointRepository pointRepository;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("포인트 조회")
    @Nested
    class GetPoint {
        @DisplayName("해당 ID 의 회원이 존재할 경우, 보유 포인트가 반환된다.")
        @Test
        void returnsPoint_whenValidUserIdIsProvided() {
            // arrange
            UserModel user = new UserModel("userId", "email@email.com", "1999-01-01");
            userRepository.save(user);
            PointModel pointModel = new PointModel(user, 10);
            pointService.charge(pointModel);

            // act
            PointModel result = pointService.findPoint(pointModel);

            // assert
            assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getPoint()).isEqualTo(10)
            );
        }

        @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.")
        @Test
        void returnsNull_whenInvalidUserIdIsProvided() {
            // arrange
            UserModel user = new UserModel("notUserId1", "email@email.com", "1999-01-01");
            PointModel pointModel = new PointModel(user, 10);

            // act
            PointModel result = pointService.findPoint(pointModel);

            // assert
            assertAll(
                () -> assertThat(result).isNull()
            );
        }

    }

    @DisplayName("포인트 충전")
    @Nested
    class ChargePoint {
        @DisplayName("존재하지 않는 유저 ID 로 충전을 시도한 경우, 실패한다.")
        @Test
        void throwsException_whenInvalidUserIdIsProvided() {
            // arrange
            UserModel user = new UserModel("notUserId1", "email@email.com", "1999-01-01");
            PointModel pointModel = new PointModel(user, 10);

            // assert
            assertThrows(CoreException.class, () -> pointService.charge(pointModel));
        }

    }
}
